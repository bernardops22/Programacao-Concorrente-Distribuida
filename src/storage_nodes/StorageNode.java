package storage_nodes;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import messages.ByteBlockRequest;
import messages.CloudByte;

public class StorageNode {

    private static final int FILE_SIZE = 1000000;
    private static final int BLOCK_SIZE = 100;
    private static final int MAX_BLOCKS = 10000;

    private final String serverAddress, path;
    private final int senderPort, receiverPort;

    private Socket socket;
    private ServerSocket serverSocket;
    private BufferedReader in;
    private PrintWriter out;

    public final CloudByte[] fileContent = new CloudByte[FILE_SIZE];
    private final List<String> nodes = new ArrayList<>();
    public BlockingQueue<ByteBlockRequest> queue = new BlockingQueue<>(MAX_BLOCKS);

    public StorageNode(String serverAddress, int senderPort, int receiverPort, String path){
        this.serverAddress = serverAddress;
        this.senderPort = senderPort;
        this.receiverPort = receiverPort;
        this.path = path;
    }

    public static void main(String[] args){
        try {
            if (args.length == 3)
                new StorageNode(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), null).runNode();
            else if (args.length == 4)
                new StorageNode(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), args[3]).runNode();
        } catch (RuntimeException e) {
            System.err.println("Problem in the arguments: Directory port and address must be written, " +
                    "followed by the node port and the data file name.");
        }
    }

    public void runNode() {
        try{
            connectToTheDirectory();
            registerInTheDirectory();
            if(path!=null)
                getFileContent();
            else {
                getNodesList();
                createQueue();
                getContentFromNodes();
            }
            new userInput().start();
            acceptingConnections();
        }catch (IOException | InterruptedException | ClassNotFoundException e){
            e.printStackTrace();
        }
    }

    private void connectToTheDirectory() throws IOException {
        InetAddress address = InetAddress.getByName(serverAddress);
        socket = new Socket(address,senderPort);
        out = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream())),
                true);
        in = new BufferedReader(new InputStreamReader(
                socket.getInputStream()));
    }

    private void registerInTheDirectory(){
        out.println("INSC " + serverAddress + " " + receiverPort);
        System.err.println("Sending to directory: INSC " + serverAddress + " " + receiverPort);
    }

    private void getFileContent(){
        try {
            byte[] fileContentsTemp = Files.readAllBytes(new File(path).toPath());
            if (fileContentsTemp.length != FILE_SIZE) throw new IOException();
            for (int i = 0; i < fileContentsTemp.length; i++)
                fileContent[i] = new CloudByte(fileContentsTemp[i]);
            System.err.println("Loaded data from file: " + fileContent.length);
        } catch (IOException e){
            System.err.println("File not valid.");
        }
    }

    private void getNodesList() throws IOException {
        if(fileContent[0] == null) {
            System.err.println("Querying directory from other nodes...");
            out.println("nodes");
            String line = in.readLine();
            while (!line.equals("end")) {
                System.err.println("Got answer: " + line);
                if (!line.equals("node " + socket.getLocalAddress().getHostAddress() + " " + receiverPort))
                    nodes.add(line);
                out.println("nodes");
                line = in.readLine();
            }
        }
    }

    private void createQueue() throws InterruptedException {
        for(int i = 0; i < FILE_SIZE; i += BLOCK_SIZE)
            queue.put(new ByteBlockRequest(i, BLOCK_SIZE));
    }

    private void getContentFromNodes() throws IOException, ClassNotFoundException, InterruptedException {
        long time = System.currentTimeMillis();
        for (String node : nodes) {
            System.err.println("Launching download thread: " + node);
            Thread thread = new DealWithRequest(this, node.split("\\s")[1], Integer.parseInt(node.split("\\s+")[2]));
            thread.start();
            if(nodes.get(nodes.size()-1) == node) thread.join(); //TODO wait for both threads to finish
        }
        System.err.println("Elapsed time: " + (System.currentTimeMillis() - time));

        //TODO get downloaded data from threads

        /*int data = 0;
        for(int i = 0; i < nodes.size(); i++) {
            data += Integer.parseInt(information.split(" ")[1]) * BLOCK_SIZE;
        }*/
        //System.err.println("Downloaded data length: " + data);
    }

    private void acceptingConnections() throws IOException, ClassNotFoundException {
        ServerSocket serverSocket = new ServerSocket(receiverPort);
        System.err.println("Accepting connections...");
        while(true){
            Socket getContentSocket = serverSocket.accept();
            ObjectOutputStream out = new ObjectOutputStream(getContentSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(getContentSocket.getInputStream());
            while(true) {
                try {
                    ByteBlockRequest request = (ByteBlockRequest) in.readObject();
                    CloudByte[] block = new CloudByte[request.getLength()];
                    int startIndex = request.getStartIndex();
                    for (int i = startIndex; i < request.getLength(); i++) {
                        block[i - startIndex] = fileContent[i];
                    }
                    out.writeObject(block);
                }catch(Exception e) {
                    break;
                }
            }
        }
    }

    private class userInput extends Thread {
        @Override
        public void run() {
            Scanner scanner = new Scanner(System.in);
            String line = scanner.nextLine();
            while (!line.equals("exit") && !line.equals("EXIT")) {
                if(line.split("\\s")[0].equals("ERROR"))
                    injectError(line.split("\\s+")[1]);
                else System.err.println("Command not recognized.");
                line = scanner.nextLine();
            }
        }
    }

    private void injectError(String position){
        fileContent[Integer.parseInt(position)-1].makeByteCorrupt();
        System.err.println("Error injected: " + fileContent[Integer.parseInt(position)-1]);
    }
}
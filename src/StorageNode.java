import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

public class StorageNode {

    private static final int FILE_SIZE = 1000000;
    private static final int BLOCK_SIZE = 100;
    private static final int MAX_BLOCKS = 10000;
    private static final int MAX_ERROR_CORRECTORS = 2;

    private final String serverAddress, path;
    private final int senderPort, receiverPort;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean correction = false;

    private final CloudByte[] fileContent = new CloudByte[FILE_SIZE];
    private final List<String> nodes = new ArrayList<>();
    private final BlockingQueue<ByteBlockRequest> queue = new ArrayBlockingQueue<>(MAX_BLOCKS);

    public StorageNode(String serverAddress, int senderPort, int receiverPort, String path){
        this.serverAddress = serverAddress;
        this.senderPort = senderPort;
        this.receiverPort = receiverPort;
        this.path = path;
    }

    public static void main(String[] args) {
        try {
            if (args.length == 3)
                new StorageNode(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), null).runNode();
            else if (args.length == 4)
                new StorageNode(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), args[3]).runNode();
        } catch(Exception e){
            System.err.println("Problem in the arguments: Directory port and address must be written, " +
                    "followed by the node port and the data file name.");
        }
    }

    private void runNode() {
        try{
            connectToTheDirectory();
            if(!registerInTheDirectory()) {
                System.err.println("Client already enrolled. Try changing port number.");
                return;
            }
            if(path!=null) {
                if (!getFileContent())
                    return;
            }
            else {
                if(fileContent[0] == null)
                    getNodesList();
                if(nodes.size() != 0) {
                    createQueue();
                    getContentFromNodes(nodes.size());
                }
                else{
                    System.err.println("No nodes available beside yours.");
                    return;
                }
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

    private boolean registerInTheDirectory() throws IOException {
        out.println("INSC " + socket.getLocalAddress().getHostAddress() + " " + receiverPort);
        System.err.println("Sending to directory: INSC " + socket.getLocalAddress().getHostAddress() + " " + receiverPort);
        return in.readLine().equals("true");
    }

    private boolean getFileContent(){
        try {
            byte[] fileContentsTemp = Files.readAllBytes(new File(path).toPath());
            if (fileContentsTemp.length != FILE_SIZE) throw new IOException();
            for (int i = 0; i < fileContentsTemp.length; i++)
                fileContent[i] = new CloudByte(fileContentsTemp[i]);
            System.err.println("Loaded data from file: " + fileContent.length);
            return true;
        } catch (IOException e){
            System.err.println("File not valid. Problem in the path or in the file content.");
            return false;
        }
    }

    private void getNodesList() throws IOException {
        nodes.clear();
        System.err.println("Querying directory for other nodes...");
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

    private void createQueue() throws InterruptedException {
        for(int i = 0; i < FILE_SIZE; i += BLOCK_SIZE)
            queue.put(new ByteBlockRequest(i, BLOCK_SIZE));
    }

    private void getContentFromNodes(int cdlSize) throws IOException, ClassNotFoundException, InterruptedException {
        long time = System.currentTimeMillis();
        CountDownLatch cdl = new CountDownLatch(cdlSize);
        for (String node : nodes) {
            if(!correction)System.err.println("Launching download thread: " + node);
            else System.err.println("Launching correction thread: " + node);
            Thread thread = new DealWithRequest(node.split(" ")[1], Integer.parseInt(node.split(" ")[2]), cdl);
            thread.start();
        }
        try{
            cdl.await();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        if(!correction) System.err.println("Elapsed time: " + (System.currentTimeMillis() - time));
        //TODO get downloaded data from threads
    }

    private class DealWithRequest extends Thread{

        private final String inAddress;
        private final int inPort;
        private final CountDownLatch cdl;
        private int numBlocks = 0;

        public DealWithRequest(String inAddress, int inPort, CountDownLatch cdl) {
            this.inAddress = inAddress;
            this.inPort = inPort;
            this.cdl = cdl;
        }

        @Override
        public void run(){
            ByteBlockRequest request = null;
            try {
                Socket socket = new Socket(inAddress, inPort);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                while (queue.size() != 0) {
                    request = queue.take();
                    out.writeObject(request);
                    try {
                        CloudByte[] block = (CloudByte[]) in.readObject();
                        for (int i = 0; i < request.getLength(); i++)
                            fileContent[i + request.getStartIndex()] = block[i];
                        numBlocks++;
                    }catch(NullPointerException e){
                        queue.add(request);
                        System.err.println("Parity error. Last request added to the queue: " + request);
                        sleep(10000); //TODO Evitar loops infinitos
                    }
                }
                out.close();
                in.close();
                socket.close();
            } catch (IOException | InterruptedException | ClassNotFoundException e) {
                if(request != null) {
                    queue.add(request);
                    System.err.println("Last request added to the queue : " + request);
                }
                System.err.println("Unable to connect to desired socket: " + inAddress + " " + inPort);
            }
            if(!correction) System.err.println("Downloaded " + numBlocks + " blocks from " + inAddress + ":" + inPort);
            //TODO try to end correction after
            cdl.countDown();
        }
    }

    private void acceptingConnections() throws IOException, ClassNotFoundException {
        ServerSocket serverSocket = new ServerSocket(receiverPort);
        System.err.println("Accepting connections...");
        for(int i = 0; i < MAX_ERROR_CORRECTORS; i++)
            new CheckForParityErrors().start();
        while(true){
            Socket getContentSocket = serverSocket.accept();
            ObjectOutputStream out = new ObjectOutputStream(getContentSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(getContentSocket.getInputStream());
            while(true) {
                try {
                    ByteBlockRequest request = (ByteBlockRequest) in.readObject();
                    int requestSize = request.getLength();
                    CloudByte[] block = new CloudByte[requestSize];
                    for (int i = 0; i < requestSize; i++)
                        block[i] = fileContent[i + request.getStartIndex()];
                    if(checkBlockForErrors(block)) out.writeObject(block);
                    else out.writeObject(null);
                }catch(Exception e) {
                    break;
                }
            }
            getContentSocket.close();
            out.close();
            in.close();
        }
    }

    private boolean checkBlockForErrors(CloudByte[] block){
        for(int i = 0; i != block.length; i++)
            if(!block[i].isParityOk())
                return false;
        return true;
    }

    private class CheckForParityErrors extends Thread {

        @Override
        public void run() {
            try {
                int start = 0;
                if (this.getId() == 1) start = fileContent.length/2;
                while (true) {
                    for (int i = start; i < fileContent.length/2; i++) {
                        CloudByte b = fileContent[i];
                        rectifyParityError(b, i);
                        sleep(1/100);
                    }
                }
            }catch (IOException | InterruptedException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void rectifyParityError(CloudByte b, int position) throws IOException, ClassNotFoundException, InterruptedException {
        if (!b.isParityOk()) {
            try {
                correction = true;
                System.err.println("Data Maintenance: Error was detected at " + (position + 1) + ": " + b);
                getNodesList();
                if (nodes.size() >= 2) {
                    for (int i = 0; i != nodes.size(); i++)
                        queue.add(new ByteBlockRequest(position, 1));
                    getContentFromNodes(2);
                    System.err.println("Corrected to: " + fileContent[position]);
                    correction = false;
                } else {
                    System.err.println("Cannot correct the error. Insufficient number of nodes.\nDisconnecting node.");
                    System.exit(-1); //TODO to correct in the future
                }
            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                e.printStackTrace(); //TODO try catch
            }
        }
    }

    private class userInput extends Thread {
        @Override
        public void run() {
            Scanner scanner = new Scanner(System.in);
            String line = scanner.nextLine();
            while (true) {
                if(line.split(" ")[0].equals("ERROR"))
                    injectError(line.split(" ")[1]);
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
package storage_nodes;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.Scanner;

import messages.CloudByte;

public class StorageNode {

    private String serverAddress, path;
    private int senderPort, receiverPort;

    private InetAddress address;
    private Socket socket;
    private ServerSocket serverSocket;
    private BufferedReader in;
    private PrintWriter out;

    private CloudByte[] fileContents = new CloudByte[1000000];

    public StorageNode(String serverAddress, int senderPort, int receiverPort, String path){
        this.serverAddress = serverAddress;
        this.senderPort = senderPort;
        this.receiverPort = receiverPort;
        this.path = path;
    }

    public static void main(String[] args) throws IOException {
        try {
            if (args.length == 3)
                new StorageNode(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), null).runNode();
            else if (args.length == 4)
                new StorageNode(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), args[3]).runNode();
        } catch (RuntimeException e) {
            System.err.println("Problema nos argumentos: Endereço e porto do Diretorio devem ser indicados, " +
                    "seguidos do porto deste nó e do nome do ficheiro de dados.");
        }
    }

    public void runNode() throws IOException {
        try{
            connectToTheDirectory();
            registerInTheDirectory();
            getFileContent();
            userInput();
        }catch (IOException e){}
        finally{
            socket.close();
        }
    }

    private void connectToTheDirectory() throws IOException {
        address = InetAddress.getByName(serverAddress);
        socket = new Socket(address,senderPort);
        serverSocket = new ServerSocket(receiverPort);
        in = new BufferedReader(new InputStreamReader(
                socket.getInputStream()));
        out = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream())),
                true);
    }

    private void registerInTheDirectory() throws UnknownHostException {
        out.println("INS " + socket.getLocalAddress().getHostAddress() + " " + serverSocket.getLocalPort());
        System.err.println("Sending to directory: INS " + socket.getLocalAddress().getHostAddress() + " " + serverSocket.getLocalPort());
    }

    private void getFileContent() throws IOException {
        if(path!=null)
            try{
                byte[] fileContentsTemp = Files.readAllBytes(new File(path).toPath());
                if(fileContentsTemp.length != 1000000) throw new IOException();
                for (int i = 0; i < fileContentsTemp.length; i++)
                    fileContents[i] = new CloudByte(fileContentsTemp[i]);
                System.err.println("Loaded data from file: " + fileContents.length);
                System.err.println("Accepting connections...");
            }catch (IOException e) {
                System.err.println("File not valid.");
                getFromOtherNodes();
            }
        else getFromOtherNodes();
    }

    public void getFromOtherNodes() throws IOException {
        System.err.println("Querying directory for other nodes...");
        sendMessage("nodes");
    }

    private void userInput() throws IOException  {
        Scanner scanner = new Scanner(System.in);
        while(true)
            sendMessage(scanner.nextLine());
    }

    private void sendMessage(String line) throws IOException {
        if(line.split("\\s")[0].equals("ERROR"))
            injectError(line.split("\\s+")[1]);
        else if(line.equals("nodes")) {
            out.println("nodes");
            System.err.println("Got answer: "+ in.readLine());
            downloadThread();
        }
        else if(line.equals("exit") || line.equals("EXIT")){
            return;
        }
        else System.err.println("Command not recognized.");
    }

    private void downloadThread() {
        System.err.println("Lauching download thread: ");
    }

    private void injectError(String position){
        fileContents[Integer.parseInt(position)-1].makeByteCorrupt();
        System.err.println("Error injected: " + fileContents[Integer.parseInt(position)-1]);
    }
}
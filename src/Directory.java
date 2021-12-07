import java.io.*;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * @author bernardosantos
 */
public class Directory {

    private final ServerSocket serverSocket;
    private final List<String> nodes = new ArrayList<>();

    public Directory(int porto) throws IOException {
        this.serverSocket = new ServerSocket(porto);
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 1)
            try {
                new Directory(Integer.parseInt(args[0])).serve();
            }catch(BindException e){
                System.err.println("Address already in use: " + args[0]);
                e.printStackTrace();
            }
        else
            throw new RuntimeException("Port number must be the first and only argument");
    }

    private void serve(){
        System.err.println("Initiating service...");
        while (true){
            try {
                Socket clientSocket = serverSocket.accept();
                new DealWithNode(clientSocket).start();
            }catch(IOException e){
                System.err.println("Error accepting client connection to the directory");
                e.printStackTrace();
            }
        }
    }

    private synchronized void addClient(String clientAddress, String clientPort){
        nodes.add(clientAddress + " " + clientPort);
        System.err.println("Client enrolled: " + clientAddress + " " + clientPort);
    }

    private synchronized void sendNodeList(PrintWriter out){
        for (String node : nodes)
            out.println("node " + node);
        out.println("end");
    }

    private synchronized void disconnectClient(String clientAddress, String clientPort){
        for(String node: nodes){
            if(node.split(" ")[0].equals(clientAddress) && node.split(" ")[1].equals(clientPort)){
                nodes.remove(node);
                System.err.println("Client disconnected: " + node);
                return;
            }
        }
    }

    private synchronized boolean isNodeEnrolled(String clientAddress, String clientPort){
        for(String node: nodes)
            if(node.split(" ")[0].equals(clientAddress) && node.split(" ")[1].equals(clientPort))
                return true;
        return false;
    }

    private class DealWithNode extends Thread{
        private final BufferedReader in;
        private final PrintWriter out;
        private String clientAddress, clientPort;

        public DealWithNode(Socket socket) throws IOException {
            in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
            out = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())),
                    true);
        }

        @Override
        public void run(){
            boolean isConnected = false;
            try {
                while(true) {
                    String request = in.readLine();
                    if(request == null){
                        break;
                    }
                    String[] requestContent = request.split(" ");
                    switch (requestContent[0]){
                        case "INSC":
                            if(requestContent.length == 3) {
                                this.clientAddress = requestContent[1];
                                this.clientPort = requestContent[2];
                                if(!isNodeEnrolled(clientAddress, clientPort)){
                                    out.println("true");
                                    addClient(clientAddress, clientPort);
                                    isConnected = true;
                                } else{
                                    out.println("false");
                                    isConnected = false;
                                }
                            }
                            else
                                System.err.println("Error receiving client enrollment.");
                            break;
                        case "nodes":
                            System.err.println("New message received: " + request);
                            sendNodeList(out);
                            break;
                        default:
                            System.err.println("Message not understood: " + request);
                            break;
                    }
                }
            } catch (IOException e) {
                System.err.println("Error establishing communication channel with the client.");
                e.printStackTrace();
            }finally {
                if(isConnected)
                    disconnectClient(clientAddress, clientPort);
            }
        }
    }
}
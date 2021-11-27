import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Directory {

    private final ServerSocket serverSocket;
    private final List<String> nodes = new ArrayList<>();

    public Directory(int porto) throws IOException {
        this.serverSocket = new ServerSocket(porto);
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 1)
            new Directory(Integer.parseInt(args[0])).serve();
        else
            throw new RuntimeException("Port number must be the first and only argument");
    }

    public void serve(){
        System.err.println("Initiating service...");
        while (true){
            try {
                Socket socket = serverSocket.accept();
                new dealWithNode(socket).start();
            }catch(IOException e){
                System.err.println("Error accepting client connection to the directory");
            }
        }
    }

    private class dealWithNode extends Thread{

        private final Socket socket;
        private final BufferedReader in;
        private final PrintWriter out;

        public dealWithNode (Socket socket) throws IOException {
            this.socket = socket;
            in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
            out = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())),
                    true);
        }

        @Override
        public void run(){
            try {
                while(true) {
                    String request = in.readLine();
                    String[] requestContent = request.split(" ");
                    switch (requestContent[0]){
                        case "INSC":
                            if(requestContent.length == 3)
                                enrollClient(requestContent[1],requestContent[2]);
                            else
                                System.err.println("Error receiving client enrollment.");
                            break;
                        case "nodes":
                            System.err.println("New message received: " + request + ".");
                            sendNodeList();
                            break;
                        default:
                            System.err.println("New message received: " + request + ".");
                            System.err.println("Message not understood: " + request + ".");
                            break;
                    }
                }
            } catch (IOException e) {
                System.err.println("Error initiating communication channel with the client.");
            }
        }

        public void enrollClient(String address, String port){
            nodes.add(address + " " + Integer.parseInt(port));
            System.err.println("Client enrolled: " + socket.getLocalAddress().getHostAddress() + " " + port + ".");
        }

        public void sendNodeList(){
            for (String node : nodes)
                out.println("node " + node);
            out.println("end");
        }
    }
}

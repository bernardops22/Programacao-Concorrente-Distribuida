import java.io.*;
import java.net.Socket;

public class DealWithClient extends Thread {

    private final Directory directory;
    private final BufferedReader in;
    private final PrintWriter out;
    private String clientAddress, clientPort;

    public DealWithClient(Directory directory, Socket socket) throws IOException {
        this.directory = directory;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),true);
    }

    @Override
    public void run(){
        boolean isConnected = false;
        try {
            while(true) {
                String request = in.readLine();
                if(request == null)
                    break;
                String[] requestContent = request.split(" ");
                switch (requestContent[0]){
                    case "INSC":
                        if(requestContent.length == 3) {
                            clientAddress = requestContent[1];
                            clientPort = requestContent[2];
                            if(!directory.isClientEnrolled(clientAddress, clientPort)){
                                out.println("true");
                                directory.addClient(clientAddress, clientPort);
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
                        directory.sendClientList(out);
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
                directory.disconnectClient(clientAddress, clientPort);
        }
    }
}
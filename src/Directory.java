import java.io.*;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.NumberFormat;
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
            } catch(BindException e){
                throw new BindException("Address already in use: " + args[0]);
            } catch (NumberFormatException e1){
                throw new NumberFormatException("Inserted port invalid: " + args[0]);
            }
        else
            throw new RuntimeException("Port number must be the first and only argument");
    }

    /**
     * Através deste método, por cada cliente que se tentar inscrever é iniciada
     * uma thread DealWithClient para que as ligações com o mesmo sejam geridas
     */
    private void serve(){
        System.err.println("Initiating service...");
        while (true){
            try {
                Socket clientSocket = serverSocket.accept();
                new DealWithClient(this,clientSocket).start();
            }catch(IOException e){
                System.err.println("Error accepting client connection to the directory");
                e.printStackTrace();
            }
        }
    }

    /**
     * Neste método o cliente que se regista é adicionado à lista de clientes
     */
    synchronized void addClient(String clientAddress, String clientPort){
        nodes.add(clientAddress + " " + clientPort);
        System.err.println("Client enrolled: " + clientAddress + " " + clientPort);
    }

    /**
     * Neste método é enviado em formato de texto o conjunto de todos os clientes na lista
     * de clientes inscritos no diretório
     */
    synchronized void sendClientList(PrintWriter out){
        for (String node : nodes)
            out.println("node " + node);
        out.println("end");
    }

    /**
     * O cliente que tiver algum problema é removido da lista de clientes inscritos
     */
    synchronized void disconnectClient(String clientAddress, String clientPort){
        for(String node: nodes){
            if(node.split(" ")[0].equals(clientAddress) && node.split(" ")[1].equals(clientPort)){
                nodes.remove(node);
                System.err.println("Client disconnected: " + node);
                return;
            }
        }
    }

    /**
     * Este método verifica se o cliente em causa está inscrito na lista
     * @return Retorna verdadeiro se o cliente estiver inscrito. Caso contrário
     * retorna falso
     */
    synchronized boolean isClientEnrolled(String clientAddress, String clientPort){
        for(String node: nodes)
            if(node.split(" ")[0].equals(clientAddress) && node.split(" ")[1].equals(clientPort))
                return true;
        return false;
    }
}
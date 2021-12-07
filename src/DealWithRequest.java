import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

/**
 * @author bernardosantos
 */
public class DealWithRequest extends Thread{

    private final StorageNode node;
    private final String inAddress;
    private final int inPort;
    private final CountDownLatch cdl;
    private int numBlocks = 0;

    public DealWithRequest(StorageNode node, String inAddress, int inPort, CountDownLatch cdl) {
        this.node = node;
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
            while (node.queue.size() != 0) {
                request = node.queue.take();
                out.writeObject(request);
                try {
                    CloudByte[] block = (CloudByte[]) in.readObject();
                    for (int i = 0; i < request.getLength(); i++)
                        node.fileContent[i + request.getStartIndex()] = block[i];
                    numBlocks++;
                }catch(NullPointerException e){
                    node.queue.add(request);
                    System.err.println("Parity error. Last request added to the queue: " + request);
                    e.printStackTrace();
                }
            }
            out.close();
            in.close();
            socket.close();
        } catch (IOException | InterruptedException | ClassNotFoundException e) {
            if(request != null) {
                node.queue.add(request);
                System.err.println("Last request added to the queue : " + request);
            }
            System.err.println("Unable to connect to desired socket: " + inAddress + " " + inPort);
            e.printStackTrace();
        }
        System.err.println("Downloaded " + numBlocks + " blocks from " + inAddress + ":" + inPort);
        cdl.countDown();
    }
}
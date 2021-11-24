package storage_nodes;

import messages.ByteBlockRequest;
import messages.CloudByte;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class DealWithRequest extends Thread{

    private StorageNode storageNode;
    private String inAddress;
    private int inPort;
    private int numBlocks = 0;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Socket socket;

    public DealWithRequest(StorageNode storageNode, String inAddress, int inPort) {
        this.storageNode = storageNode;
        this.inAddress = inAddress;
        this.inPort = inPort;
    }

    @Override
    public void run(){
        try {
            socket = new Socket(inAddress, inPort);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            serve();
            System.err.println("Downloaded " + numBlocks + " blocks from " + inAddress + ":" + inPort);
            socket.close();
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void serve () throws IOException, ClassNotFoundException, InterruptedException {
        while (storageNode.queue.size() != 0) {
            ByteBlockRequest request = storageNode.queue.take();
            final int startIndex = request.getStartIndex();
            final int lastIndex = startIndex + request.getLength();
            out.writeObject(request);
            CloudByte[] block = (CloudByte[]) in.readObject();
            int j = 0;
            for(int i = startIndex; i < lastIndex; i++) {
                storageNode.fileContent[i] = block[j];
                j++;
            }
            numBlocks++;
        }
    }
}

import java.io.IOException;
import java.util.concurrent.locks.Lock;

/**
 * @author bernardosantos
 */
public class CheckForParityErrors extends Thread {

    private final StorageNode node;
    private final int threadNumber;
    private final Lock lock;

    public CheckForParityErrors(StorageNode node, int threadNumber, Lock lock){
        this.node = node;
        this.threadNumber = threadNumber;
        this.lock = lock;
    }

    @Override
    public void run() {
        try {
            int fileLength = node.fileContent.length;
            int init;
            int fin;
            if(threadNumber == 0){
                init = 0;
                fin = fileLength/2;
            }
            else{
                init = fileLength/2;
                fin = fileLength;
            }
            while (true)
                for(int i = init; i < fin; i++) {
                    if (!node.fileContent[i].isParityOk()) {
                        boolean canLock = lock.tryLock();
                        if(canLock) {
                            lock.lock();
                            node.startErrorCorrection(i);
                            node.correctError(i);
                            lock.unlock();
                        }
                    }
                }
        } catch (IOException | InterruptedException | ClassNotFoundException e) {
            //...
        }
    }
}
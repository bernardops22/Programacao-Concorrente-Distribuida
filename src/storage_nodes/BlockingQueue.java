package storage_nodes;

import java.util.LinkedList;
import java.util.Queue;

public class BlockingQueue<T> {

    private final Queue<T> queue;
    private int capacity;

    public BlockingQueue(int capacity) {
        this.queue = new LinkedList<>();
        this.capacity = capacity;
    }

    public synchronized void put(T elem) throws InterruptedException{
        while(queue.size() == capacity)
            wait();
        queue.offer(elem);
        notifyAll();
    }

    public synchronized T take() throws InterruptedException{
        while(queue.isEmpty())
            wait();
        T elem = queue.poll();
        notifyAll();
        return elem;
    }

    public int size() {
        return queue.size();
    }

    public void clear() {
        queue.clear();
    }
}

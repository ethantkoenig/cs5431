package network;

import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

/**
 * Created by EvanKing on 3/3/17.
 */
public class BroadcastThread extends Thread{

    private static final Logger LOGGER = Logger.getLogger(BroadcastThread.class.getName());
    private Node parentNode;

    private BlockingQueue<Message> broadcastQueue;

    public BroadcastThread(Node parentNode, BlockingQueue<Message> broadcastQueue){
        this.parentNode = parentNode;
        this.broadcastQueue = broadcastQueue;
    }

    /**
     * The run() function is ran when the thread is started. We pull off of the synchronized blocking broadcastQueue
     * whenever there is a broadcast message to be pulled. We then broadcast the message to all connected nodes.
     */
    @Override
    public void run() {
        try {
            Message message;
            while ((message = broadcastQueue.take()) != null) {
                synchronized(parentNode) {
//                    parentNode.broadcast(message);
                }
            }
        } catch (InterruptedException e) {
            LOGGER.severe(e.getMessage());
        }
    }
}

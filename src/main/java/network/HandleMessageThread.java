package network;

import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

/**
 * The network.HandleMessageThread is a background thread ran by the instantiated Node class
 * in order to process incoming messages from all connected nodes.
 *
 * @author Evan King
 * @version 1.0, Feb 22 2017
 */
public class HandleMessageThread extends Thread {
    private static final Logger LOGGER = Logger.getLogger(HandleMessageThread.class.getName());

    private BlockingQueue<Message> queue;

    public HandleMessageThread(BlockingQueue<Message> queue) {
        this.queue = queue;
    }

    /**
     * The run() function is ran when the thread is started. We pull off of the synchronized blocking queue
     * whenever there is a message to be pulled. We then consume this message appropriately.
     */
    @Override
    public void run() {
        try {
            Message message;
            while ((message = queue.take()) != null) {
                switch (message.type) {
                    case Message.TRANSACTION:
                        // TODO
                        break;
                    case Message.BLOCK:
                        // TODO
                        break;
                    default:
                        LOGGER.severe(String.format("Unexpected message type: %d", message.type));
                }
            }
        } catch (InterruptedException e) {
            LOGGER.severe(e.getMessage());
        }
    }

}

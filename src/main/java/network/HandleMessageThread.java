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

    private BlockingQueue<Message> messageQueue;
    private BlockingQueue<Message> broadcastQueue;

    // Needs reference to parent in order to call Node.broadcast()
    public HandleMessageThread(BlockingQueue<Message> messageQueue, BlockingQueue<Message> broadcastQueue) {
        this.messageQueue = messageQueue;
        this.broadcastQueue = broadcastQueue;
    }


    /**
     * The run() function is ran when the thread is started. We pull off of the synchronized blocking messageQueue
     * whenever there is a message to be pulled. We then consume this message appropriately.
     */
    @Override
    public void run() {
        try {
            Message message;
            while ((message = messageQueue.take()) != null) {
                switch (message.type) {
                    case Message.TRANSACTION:
                        // TODO: check that we haven't yet received this message then:
                        // broadcastQueue.put(message);
                        // TODO: add transaction to working block then starting mining thread with block
                        // new MinerThread(block, broadcastQueue).start();
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

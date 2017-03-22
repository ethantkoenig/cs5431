package network;

import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * The network.BroadcastThread class extends Thread and is started by a node in order to broadcast messages.
 * It pulls messages off of a synchronized queue broadcastQueue which has messages put on it by both the
 * network.MinerThread and the network.HandleMessageThread.
 *
 * @version 1.0, March 1 2017
 */
public class BroadcastThread extends Thread {

    private static final Logger LOGGER = Logger.getLogger(BroadcastThread.class.getName());

    private final Consumer<OutgoingMessage> broadcast;

    private final BlockingQueue<OutgoingMessage> broadcastQueue;

    // Needs reference to parent in order to call Node.broadcast()
    public BroadcastThread(Consumer<OutgoingMessage> broadcast, BlockingQueue<OutgoingMessage> broadcastQueue) {
        this.broadcast = broadcast;
        this.broadcastQueue = broadcastQueue;
    }

    /**
     * The run() function is ran when the thread is started. We pull off of the synchronized blocking broadcastQueue
     * whenever there is a broadcast message to be pulled. We then broadcast the message to all connected nodes.
     */
    @Override
    public void run() {
        try {
            OutgoingMessage message;
            while ((message = broadcastQueue.take()) != null) {
                broadcast.accept(message);
            }
        } catch (InterruptedException e) {
            LOGGER.severe(e.getMessage());
        }
    }
}

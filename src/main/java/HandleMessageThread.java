import java.util.concurrent.BlockingQueue;

/**
 * The HandleMessageThread is a background thread ran by the instantiated Node class
 * in order to process incoming messages from all connected nodes.
 *
 * @author Evan King
 * @version 1.0, Feb 22 2017
 */
public class HandleMessageThread extends Thread {

    private BlockingQueue<String> queue;

    public HandleMessageThread(BlockingQueue<String> queue) {
        this.queue = queue;
    }

    /**
     * The run() function is ran when the thread is started. We pull off of the synchronized blocking queue
     * whenever there is a message to be pulled. We then consume this message appropriately.
     */
    @Override
    public void run() {
        try {
            String message;
            //consuming messages is just printing them for now
            while ((message = queue.take()) != null) {
                System.out.println("Consumed: " + message);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

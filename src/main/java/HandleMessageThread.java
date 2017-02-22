import java.util.concurrent.BlockingQueue;

/**
 * Created by EvanKing on 2/21/17.
 */
public class HandleMessageThread extends Thread {

    private BlockingQueue<String> queue;

    public HandleMessageThread(BlockingQueue<String> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            String message;
            //consuming messages until exit message is received
            while ((message = queue.take()) != null) {
                System.out.println("Consumed " + message);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

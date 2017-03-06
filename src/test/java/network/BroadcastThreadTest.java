package network;

import org.junit.Assert;
import org.junit.Test;
import testutils.RandomizedTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class BroadcastThreadTest extends RandomizedTest {

    @Test
    public void testBroadcastThread() {
        List<Message> messages = new ArrayList<>();

        BlockingQueue<Message> queue = new ArrayBlockingQueue<Message>(10);
        BroadcastThread thread = new BroadcastThread(messages::add, queue);
        thread.start();

        Message m1 = new Message(Message.BLOCK, randomBytes(random.nextInt(1024)));
        Message m2 = new Message(Message.BLOCK, randomBytes(random.nextInt(1024)));
        Message m3 = new Message(Message.TRANSACTION, randomBytes(random.nextInt(1024)));
        queue.add(m1);
        queue.add(m2);
        queue.add(m3);
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertEquals(errorMessage, messages, Arrays.asList(m1, m2, m3));
    }
}

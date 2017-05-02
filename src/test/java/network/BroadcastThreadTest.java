package network;

import message.Message;
import message.OutgoingMessage;
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
    public void testBroadcastThread() throws Exception {
        BlockingQueue<OutgoingMessage> inQueue = new ArrayBlockingQueue<>(5);
        BlockingQueue<OutgoingMessage> outQueue = new ArrayBlockingQueue<>(5);
        new BroadcastThread(inQueue::add, outQueue).start();

        OutgoingMessage m1 = new OutgoingMessage(Message.BLOCKS, randomBytes(random.nextInt(1024)));
        OutgoingMessage m2 = new OutgoingMessage(Message.BLOCKS, randomBytes(random.nextInt(1024)));
        OutgoingMessage m3 = new OutgoingMessage(Message.TRANSACTION, randomBytes(random.nextInt(1024)));
        outQueue.add(m1);
        outQueue.add(m2);
        outQueue.add(m3);

        List<OutgoingMessage> messages = new ArrayList<>();
        messages.add(inQueue.take());
        messages.add(inQueue.take());
        messages.add(inQueue.take());
        Assert.assertEquals(errorMessage, messages, Arrays.asList(m1, m2, m3));
    }
}

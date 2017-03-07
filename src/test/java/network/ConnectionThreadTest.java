package network;

import org.junit.Assert;
import org.junit.Test;
import testutils.RandomizedTest;
import testutils.TestUtils;
import utils.Pair;

import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ConnectionThreadTest extends RandomizedTest {

    @Test
    public void testRun() throws Exception {
        Pair<Socket, Socket> pair = TestUtils.sockets(10000);

        BlockingQueue<Message> leftQueue = new ArrayBlockingQueue<Message>(5);
        ConnectionThread leftThread = new ConnectionThread(pair.getLeft(), leftQueue);
        leftThread.start();

        BlockingQueue<Message> rightQueue = new ArrayBlockingQueue<Message>(5);
        ConnectionThread rightThread = new ConnectionThread(pair.getRight(), rightQueue);
        rightThread.start();

        Message m1 = new Message(Message.BLOCK, randomBytes(random.nextInt(1024)));
        Message m2 = new Message(Message.BLOCK, randomBytes(random.nextInt(1024)));
        Message m3 = new Message(Message.TRANSACTION, randomBytes(random.nextInt(1024)));
        leftThread.send(m1.type, m1.payload);
        rightThread.send(m2.type, m2.payload);
        leftThread.send(m3.type, m3.payload);

        Assert.assertEquals(errorMessage, rightQueue.take(), m1);
        Assert.assertEquals(errorMessage, leftQueue.take(), m2);
        Assert.assertEquals(errorMessage, rightQueue.take(), m3);

        leftThread.close();
        rightThread.close();
    }
}

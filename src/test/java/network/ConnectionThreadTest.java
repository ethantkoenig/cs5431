package network;

import message.IncomingMessage;
import message.Message;
import message.OutgoingMessage;
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
        Pair<Socket, Socket> pair = TestUtils.sockets();

        BlockingQueue<IncomingMessage> leftQueue = new ArrayBlockingQueue<>(5);
        Connection leftConnection = Connection.connect(pair.getLeft(), true);
        ConnectionThread leftThread = new ConnectionThread(leftConnection, leftQueue);
        leftThread.start();

        BlockingQueue<IncomingMessage> rightQueue = new ArrayBlockingQueue<>(5);
        Connection rightConnection = Connection.accept(pair.getRight());
        ConnectionThread rightThread = new ConnectionThread(rightConnection, rightQueue);
        rightThread.start();

        OutgoingMessage m1 = new OutgoingMessage(Message.BLOCKS, randomBytes(random.nextInt(1024)));
        OutgoingMessage m2 = new OutgoingMessage(Message.BLOCKS, randomBytes(random.nextInt(1024)));
        OutgoingMessage m3 = new OutgoingMessage(Message.TRANSACTION, randomBytes(random.nextInt(1024)));
        leftThread.send(m1);
        rightThread.send(m2);
        leftThread.send(m3);

        IncomingMessage in1 = rightQueue.take();
        Assert.assertEquals(errorMessage, m1.type, in1.type);
        Assert.assertArrayEquals(errorMessage, m1.payload, in1.payload);

        IncomingMessage in2 = leftQueue.take();
        Assert.assertEquals(errorMessage, m2.type, in2.type);
        Assert.assertArrayEquals(errorMessage, m2.payload, in2.payload);

        IncomingMessage in3 = rightQueue.take();
        Assert.assertEquals(errorMessage, m3.type, in3.type);
        Assert.assertArrayEquals(errorMessage, m3.payload, in3.payload);

        leftThread.close();
        rightThread.close();
    }
}

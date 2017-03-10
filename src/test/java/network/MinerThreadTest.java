package network;

import block.Block;
import org.junit.Assert;
import org.junit.Test;
import testutils.RandomizedTest;
import utils.ByteUtil;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class MinerThreadTest extends RandomizedTest {

    @Test
    public void test() throws Exception {
        Block block = randomBlock(randomShaTwoFiftySix());
        BlockingQueue<Message> queue = new ArrayBlockingQueue<>(5);
        MinerThread minerThread = new MinerThread(block, queue);

        minerThread.start();
        Message msg = queue.take();
        Assert.assertEquals(errorMessage, msg.type, Message.BLOCK);

        Assert.assertTrue(errorMessage, block.checkHash());

        Assert.assertArrayEquals(errorMessage,
                msg.payload,
                ByteUtil.asByteArray(block::serialize));
    }

    @Test
    public void testStop() throws Exception {
        Block block = randomBlock(randomShaTwoFiftySix());
        BlockingQueue<Message> queue = new ArrayBlockingQueue<>(5);
        MinerThread minerThread = new MinerThread(block, queue);

        minerThread.start();
        minerThread.stopMining();
        minerThread.join();
    }
}

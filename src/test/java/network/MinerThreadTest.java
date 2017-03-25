package network;

import block.Block;
import org.junit.Assert;
import org.junit.Test;
import testutils.RandomizedTest;
import utils.ByteUtil;
import utils.CanBeSerialized;
import utils.Config;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class MinerThreadTest extends RandomizedTest {

    @Test
    public void test() throws Exception {
        Config.HASH_GOAL.set(1);
        Block block = randomBlock(randomShaTwoFiftySix());
        BlockingQueue<OutgoingMessage> queue = new ArrayBlockingQueue<>(5);
        MinerThread minerThread = new MinerThread(block, queue);

        minerThread.start();
        Message msg = queue.take();
        Assert.assertEquals(errorMessage, msg.type, Message.BLOCK);

        Assert.assertTrue(errorMessage, block.checkHash());

        Assert.assertArrayEquals(errorMessage,
                msg.payload,
                ByteUtil.asByteArray(out -> CanBeSerialized.serializeSingleton(out, block))
        );
    }

    @Test
    public void testStop() throws Exception {
        Config.HASH_GOAL.set(10); // really big, so miner won't succeed
        Block block = randomBlock(randomShaTwoFiftySix());
        BlockingQueue<OutgoingMessage> queue = new ArrayBlockingQueue<>(5);
        MinerThread minerThread = new MinerThread(block, queue);

        minerThread.start();
        minerThread.stopMining();
        minerThread.join();
    }
}

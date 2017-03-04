package network;

import block.Block;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

/**
 * The MinerThread is run as a background thread by a Node and is responsible for mining.
 * This entails constantly looking for the correct nonce in order to generate a sufficiently small
 * hash value.
 *
 * @author Evan King
 * @version 1.0, Feb 22 2017
 */
public class MinerThread extends Thread {

    private static final Logger LOGGER = Logger.getLogger(MinerThread.class.getName());
    private Block block;

    private final BlockingQueue<Message> broadcastQueue;

    public MinerThread(Block block, BlockingQueue<Message> broadcastQueue) {
        this.block = block;
        this.broadcastQueue = broadcastQueue;
    }

    /**
     * Try a random nonce then increase by one after each unsuccessful hash until
     * block mined successfully.
     *
     * @throws IOException if error hashing block
     */
    private Block tryNonces() throws Exception {
        block.setRandomNonce();
        while (true) {
            if (block.checkHash())
                return block;
            block.nonceAddOne();
        }
    }

    @Override
    public void run() {
        Block finalBlock = null;
        try {
            finalBlock = tryNonces();
        } catch (Exception e) {
            LOGGER.severe("Error hashing block: " + e.getMessage());
        }

        assert finalBlock != null;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            finalBlock.serialize(new DataOutputStream(outputStream));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Message message = new Message((byte) 1, outputStream.toByteArray());

        // Put message on broadcast queue
        try {
            broadcastQueue.put(message);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


}

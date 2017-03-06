package network;

import block.Block;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Random;
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
    private boolean stopMining = false;

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
        LOGGER.info("[!] Trying nonces...");
        block.setRandomNonce(new Random());

        while (true) {
            if (stopMining)
                return null;
            if (block.checkHash())
                return block;
            block.nonceAddOne();
        }
    }

    public void stopMining(){
        stopMining = true;
    }

    @Override
    public void run() {
        LOGGER.info("[+] MiningThread started");

        Block finalBlock = null;
        try {
            finalBlock = tryNonces();
        } catch (Exception e) {
            LOGGER.severe("Error hashing block: " + e.getMessage());
        }

        // The thread was told to stop by parent so get out.
        if (finalBlock == null){
            return;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            finalBlock.serialize(new DataOutputStream(outputStream));
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOGGER.info("[+] Successfully mined block! Broadcasting to other nodes.");
        // Put message on broadcast queue
        try {
            Message message = new Message(Message.BLOCK, outputStream.toByteArray());
            broadcastQueue.put(message);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


}

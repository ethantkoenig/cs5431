package network;

import block.Block;
import message.Message;
import message.OutgoingMessage;
import utils.ByteUtil;
import utils.Log;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static utils.CanBeSerialized.serializeSingleton;

/**
 * The MinerThread is run as a background thread by a Node and is responsible for mining.
 * This entails constantly looking for the correct nonce in order to generate a sufficiently small
 * hash value.
 *
 * @version 1.0, Feb 22 2017
 */
public class MinerThread extends Thread {
    private static final Log LOGGER = Log.forClass(MinerThread.class);
    private Block block;
    private final AtomicBoolean stopMining = new AtomicBoolean(false);

    private final BlockingQueue<OutgoingMessage> broadcastQueue;

    public MinerThread(Block block, BlockingQueue<OutgoingMessage> broadcastQueue) {
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
        if (!block.findValidNonce(stopMining)) {
            return null;
        }
        return block;
    }

    public void stopMining() {
        stopMining.set(true);
    }

    @Override
    public void run() {
        LOGGER.info("[+] MiningThread started");

        Block finalBlock = null; // TODO give this var a better name
        try {
            finalBlock = tryNonces();
        } catch (Exception e) {
            LOGGER.severe("Error hashing block: " + e.getMessage());
        }

        // The thread was told to stop by parent so get out.
        if (finalBlock == null) {
            return;
        }

        LOGGER.info("[+] Successfully mined block! Broadcasting to other nodes.");
        // Put message on broadcast queue
        try {
            final Block minedBlock = finalBlock;
            byte[] payload = ByteUtil.asByteArray(out -> serializeSingleton(out, minedBlock));
            broadcastQueue.put(new OutgoingMessage(Message.BLOCKS, payload));
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }

    }
}

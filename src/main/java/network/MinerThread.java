package network;

import block.Block;
import message.OutgoingMessage;
import message.payloads.BlocksPayload;
import utils.Log;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The MinerThread is run as a background thread by a Node and is responsible for mining.
 * This entails constantly looking for the correct nonce in order to generate a sufficiently small
 * hash value.
 *
 * @version 1.0, Feb 22 2017
 */
public class MinerThread extends Thread {
    private final Log LOGGER;
    private final Block block;
    private final AtomicBoolean stopMining = new AtomicBoolean(false);

    private final BlockingQueue<OutgoingMessage> broadcastQueue;

    public MinerThread(String name, Block block, BlockingQueue<OutgoingMessage> broadcastQueue) {
        LOGGER = Log.forClass(MinerThread.class, name);
        this.block = block;
        this.broadcastQueue = broadcastQueue;
    }

    public void stopMining() {
        stopMining.set(true);
    }

    @Override
    public void run() {
        LOGGER.info("[+] MiningThread started");
        try {
            if (block.findValidNonce(stopMining)) {
                LOGGER.info("[+] Successfully mined block! Broadcasting to other nodes.");
                broadcastQueue.put(new BlocksPayload(block).toMessage());
            }
        } catch (InterruptedException | IOException e) {
            LOGGER.severe("Error while mining: %s", e.getMessage());
        }
    }
}

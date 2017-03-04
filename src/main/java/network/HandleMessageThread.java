package network;

import block.Block;
import transaction.RTransaction;
import utils.ByteUtil;
import utils.ShaTwoFiftySix;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

/**
 * The network.HandleMessageThread is a background thread ran by the instantiated Node class
 * in order to process incoming messages from all connected nodes.
 *
 * @author Evan King
 * @version 1.0, Feb 22 2017
 */
public class HandleMessageThread extends Thread {
    private static final Logger LOGGER = Logger.getLogger(HandleMessageThread.class.getName());

    private BlockingQueue<Message> messageQueue;
    private BlockingQueue<Message> broadcastQueue;

    // The incomplete block to add incoming transactions to
    private Block currentAddToBlock;

    // The block currently being mined by the mining thread
    private Block currentHashingBlock;

    private MinerThread minerThread;

    // Needs reference to parent in order to call Node.broadcast()
    public HandleMessageThread(BlockingQueue<Message> messageQueue, BlockingQueue<Message> broadcastQueue) {
        this.messageQueue = messageQueue;
        this.broadcastQueue = broadcastQueue;
    }


    /**
     * The run() function is ran when the thread is started. We pull off of the synchronized blocking messageQueue
     * whenever there is a message to be pulled. We then consume this message appropriately.
     */
    @Override
    public void run() {
        try {
            Message message;
            while ((message = messageQueue.take()) != null) {
                switch (message.type) {
                    case Message.TRANSACTION:
                        RTransaction transaction = RTransaction.deserialize(ByteBuffer.wrap(message.payload));
                        // TODO: check that we haven't yet received this transaction then:
                        broadcastQueue.put(message);
                        // TODO: add transaction to working block then start mining thread with block

                        break;
                    case Message.BLOCK:
                        Block block = Block.deserialize(ByteBuffer.wrap(message.payload));
                        // TODO: compare this received block to currently hashed block and get difference in transactions
                        if (block.checkHash()) {
                            //TODO: add to blockchain
                        }
                        break;
                    default:
                        LOGGER.severe(String.format("Unexpected message type: %d", message.type));
                }
            }
        } catch (InterruptedException e) {
            LOGGER.severe(e.getMessage());
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startMiningThread(Block block) {
        currentHashingBlock = block;
        // If there is no current miner thread then start a new one.
        if (minerThread != null && !minerThread.isAlive()) {
            MinerThread minerThread = new MinerThread(block, broadcastQueue);
            minerThread.start();
        } else {
            //TODO: put block on a queue to mine.
        }

    }

    private void addTransactionToBlock(RTransaction transaction) {
        if (currentAddToBlock == null) {
            ShaTwoFiftySix previousBlockHash = null;
            try {
                //TODO: where to get actual previous blocks hash?
                previousBlockHash = ShaTwoFiftySix.hashOf(ByteUtil.hexStringToByteArray("test"));
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            }
            currentAddToBlock = Block.empty(previousBlockHash);
        }

        if (currentAddToBlock.isFull()) {
            // currentAddToBlock is full, so start mining it
            startMiningThread(currentAddToBlock);
            currentAddToBlock = null;
        } else {
            //currentAddToBlock is not full, so add the transaction
            currentAddToBlock.addTransaction(transaction);
        }
    }

}

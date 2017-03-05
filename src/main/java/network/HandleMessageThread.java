package network;

import block.Block;
import block.BlockChain;
import block.UnspentTransactions;
import transaction.RTransaction;
import utils.ByteUtil;
import utils.ShaTwoFiftySix;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.LinkedList;
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
    private LinkedList<Block> miningQueue;

    // The incomplete block to add incoming transactions to
    private Block currentAddToBlock;

    // The block currently being mined by the mining thread
    private Block currentHashingBlock;

    private MinerThread minerThread;

    private BlockChain blockChain;

    private UnspentTransactions unspentTransactions;

    private FixedSizeSet<RTransaction> recentTransactionsReceived;

    // Needs reference to parent in order to call Node.broadcast()
    public HandleMessageThread(BlockingQueue<Message> messageQueue, BlockingQueue<Message> broadcastQueue, BlockChain blockChain, UnspentTransactions unspentTransactions) {
        this.messageQueue = messageQueue;
        this.broadcastQueue = broadcastQueue;
        this.miningQueue = new LinkedList<>();
        this.recentTransactionsReceived = new FixedSizeSet<>();
        this.blockChain = blockChain;
        this.unspentTransactions = unspentTransactions;
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
                        // TODO: override transaction equals method
                        if (!recentTransactionsReceived.contains(transaction)) {
                            broadcastQueue.put(message);
                        }
                        recentTransactionsReceived.add(transaction);
                        addTransactionToBlock(transaction);
                        break;
                    case Message.BLOCK:
                        Block block = Block.deserialize(ByteBuffer.wrap(message.payload));
                        if (block.checkHash()) {
                            addBlockToChain(block);
                        } else {
                            LOGGER.info("Received block that does not pass hash check. Not adding to block chain.");
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


    private void startMiningThread() {
        if (miningQueue.isEmpty()) return;

        Block block = miningQueue.removeLast();
        // If there is no current miner thread then start a new one.
        if (minerThread != null && !minerThread.isAlive()) {
            currentHashingBlock = block;
            minerThread = new MinerThread(block, broadcastQueue);
            minerThread.start();
        }
    }

    private void addTransactionToBlock(RTransaction transaction) throws GeneralSecurityException, IOException {
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
            miningQueue.addFirst(currentAddToBlock);
            currentAddToBlock = null;
            startMiningThread();
            addTransactionToBlock(transaction);
        } else {
            //verify transaction
            transaction.verify(unspentTransactions);
            currentAddToBlock.addTransaction(transaction);
        }
    }

    private void addBlockToChain(Block block) {
        ArrayList<RTransaction> difference = block.getTransactionDifferences(currentHashingBlock);

        //interrupt the mining thread
        minerThread.interrupt();
        for (RTransaction transaction : difference) {
            currentAddToBlock.addTransaction(transaction);
        }

        // Add block to chain
        blockChain.insertBlock(block);

        startMiningThread();
    }

}

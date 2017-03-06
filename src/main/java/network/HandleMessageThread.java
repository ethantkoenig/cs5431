package network;

import block.Block;
import transaction.RTransaction;
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

    private MiningBundle miningBundle;

    private FixedSizeSet<Message> recentTransactionsReceived;

    // Needs reference to parent in order to call Node.broadcast()
    public HandleMessageThread(BlockingQueue<Message> messageQueue, BlockingQueue<Message> broadcastQueue, MiningBundle miningBundle) {
        this.messageQueue = messageQueue;
        this.broadcastQueue = broadcastQueue;
        this.miningQueue = new LinkedList<>();
        this.recentTransactionsReceived = new FixedSizeSet<>();
        this.miningBundle = miningBundle;
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
                        System.out.println("Received transaction!");
                        if (!recentTransactionsReceived.contains(message)) {
                            System.out.println("Received transaction and am broadcasting it!");
                            broadcastQueue.put(message);
                        }
                        recentTransactionsReceived.add(message);
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
        if (minerThread == null || !minerThread.isAlive()) {
            currentHashingBlock = block;
            block.addReward(miningBundle.getKeyPair().getPublic());
            minerThread = new MinerThread(block, broadcastQueue);
            minerThread.start();
        }
    }

    private void addTransactionToBlock(RTransaction transaction) throws GeneralSecurityException, IOException {
        if (currentAddToBlock == null) {
            System.out.println("Creating block to put transaction on.");
            ShaTwoFiftySix previousBlockHash = miningBundle.getBlockChain().getCurrentHead().getShaTwoFiftySix();
            currentAddToBlock = Block.empty(previousBlockHash);
        }

        if (currentAddToBlock.isFull()) {
            System.out.println("currentAddToBlock is Full!!");
            // currentAddToBlock is full, so start mining it
            System.out.println("starting mining thread");

            miningQueue.addFirst(currentAddToBlock);
            currentAddToBlock = null;
            startMiningThread();
            addTransactionToBlock(transaction);
        } else {
            //verify transaction
            System.out.println("verifying transaction.");
            if (transaction.verify(miningBundle.getUnspentTransactions())){
                System.out.println("adding transaction to block.");
                currentAddToBlock.addTransaction(transaction);
            } else {
                LOGGER.severe("The received transaction was not verified! Not adding to block.");
            }
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
        miningBundle.getBlockChain().insertBlock(block);

        startMiningThread();
    }

}

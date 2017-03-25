package network;

import block.Block;
import block.UnspentTransactions;
import transaction.Transaction;
import utils.ShaTwoFiftySix;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;
import java.io.DataInputStream;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Optional;
/**
 * The network.HandleMessageThread is a background thread ran by the instantiated Node class
 * in order to process incoming messages from all connected nodes.
 *
 * @version 1.0, Feb 22 2017
 */
public class HandleMessageThread extends Thread {
    // Blocks to request when we are behind other nodes
    private static final int BLOCKS_TO_GET = 100;
    private static final Logger LOGGER = Logger.getLogger(HandleMessageThread.class.getName());

    private BlockingQueue<IncomingMessage> messageQueue;
    private BlockingQueue<OutgoingMessage> broadcastQueue;
    private LinkedList<Block> miningQueue;

    // The incomplete block to add incoming transactions to
    private Block currentAddToBlock;

    // The block currently being mined by the mining thread
    private Block currentHashingBlock;

    private MinerThread minerThread;

    private MiningBundle miningBundle;

    private FixedSizeSet<IncomingMessage> recentTransactionsReceived;

    // Needs reference to parent in order to call Node.broadcast()
    public HandleMessageThread(BlockingQueue<IncomingMessage> messageQueue, BlockingQueue<OutgoingMessage> broadcastQueue, MiningBundle miningBundle) {
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
            IncomingMessage message;
            while ((message = messageQueue.take()) != null) {
                switch (message.type) {
                    case Message.TRANSACTION:
                        txMsgHandler(message);
                        break;
                    case Message.BLOCK:
                        blockMsgHandler(message);
                        break;
                    case Message.GET_BLOCK:
                        getBlockMsgHandler(message);
                        break;
                    case Message.GET_HEAD:
                        getHeadMsgHandler(message);
                        break;
                    case Message.HEAD:
                        headMsgHandler(message);
                        break;
                    default:
                        LOGGER.severe(String.format("Unexpected message type: %d", message.type));
                }
            }
        } catch (InterruptedException | GeneralSecurityException | IOException e) {
            e.printStackTrace();
            LOGGER.severe("Error receiving and/or handling message: " + e.getMessage());
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

    private void addTransactionToBlock(Transaction transaction) throws GeneralSecurityException, IOException {
        if (currentAddToBlock == null) {
            if (miningBundle.getBlockChain().getCurrentHead() == null) {
                LOGGER.warning("Received transaction before genesis block received");
                return;
            }
            LOGGER.info("[!] Creating block to put transaction on.");
            ShaTwoFiftySix previousBlockHash = miningBundle.getBlockChain().getCurrentHead().getShaTwoFiftySix();
            currentAddToBlock = Block.empty(previousBlockHash);
        }

        //verify transaction
        LOGGER.info("[!] Verifying transaction.");
        UnspentTransactions copy = miningBundle.getUnspentTransactions().copy();
        if (transaction.verify(copy)) {
            miningBundle.setUnspentTransactions(copy);
            LOGGER.info("[!] Transaction verified. Adding transaction to block.");
            LOGGER.info(transaction.toString());
            currentAddToBlock.addTransaction(transaction);
        } else {
            LOGGER.severe("The received transaction was not verified! Not adding to block.");
        }
        if (currentAddToBlock.isFull()) {
            // currentAddToBlock is full, so start mining it
            LOGGER.info("[+] Starting mining thread");

            miningQueue.addFirst(currentAddToBlock);
            currentAddToBlock = null;
            startMiningThread();
            addTransactionToBlock(transaction);
        }
    }

    private boolean addBlockToChain(Block block) {
        boolean toReturn = false;
        if (currentAddToBlock == null) {
            if (block.verifyGenesis(miningBundle.privilegedKey)) {
                LOGGER.info("Received the genesis block");
                LOGGER.info(String.format("Genesis hash: %s", block.getShaTwoFiftySix().toString()));
                toReturn = miningBundle.getBlockChain().insertBlock(block);
                currentHashingBlock = miningBundle.getBlockChain().getCurrentHead();
                LOGGER.info("[!] Creating block to put transaction on.");
                ShaTwoFiftySix previousBlockHash = miningBundle.getBlockChain().getCurrentHead().getShaTwoFiftySix();
                currentAddToBlock = Block.empty(previousBlockHash);
                miningBundle.getUnspentTransactions().put(block.getShaTwoFiftySix(), 0, block.reward);
            } else {
                LOGGER.info("Received invalid genesis block");
            }
            return toReturn;
        }
        if (currentHashingBlock != null) {
            ArrayList<Transaction> difference = block.getTransactionDifferences(currentHashingBlock);
            for (Transaction transaction : difference) {
                currentAddToBlock.addTransaction(transaction);
            }
        }

        LOGGER.info(String.format("Received valid block: hash=%s", block.getShaTwoFiftySix()));
        //interrupt the mining thread
        if (minerThread != null && minerThread.isAlive()) {
            LOGGER.info("[-] Received block. Stopping current mining thread.");
            minerThread.stopMining();
        }

        // Add block to chain
        LOGGER.info("[+] Adding completed block to block chain");
        toReturn = miningBundle.getBlockChain().insertBlock(block);
        miningBundle.getUnspentTransactions().put(block.getShaTwoFiftySix(), 0, block.reward);

        startMiningThread();
        return toReturn;
    }

    private void txMsgHandler(IncomingMessage msg)
        throws GeneralSecurityException, InterruptedException, IOException {
        Transaction transaction = Transaction.deserialize(msg.payload);
        LOGGER.info("[!] Received transaction!");
        if (!recentTransactionsReceived.contains(msg)) {
            LOGGER.info("[!] New transaction, so I am broadcasting to all other miners.");
            broadcastQueue.put(new OutgoingMessage(msg.type, msg.payload));
        } else {
            return;
        }
        recentTransactionsReceived.add(msg);
        addTransactionToBlock(transaction);
        return;
    }

    private void blockMsgHandler(IncomingMessage message)
        throws GeneralSecurityException, InterruptedException, IOException {
        Optional<Block[]> optParent = Block.deserializeBlocks(message.payload);
        if (optParent.isPresent()) {
            Block[] blocks = optParent.get();
            for (Block b : blocks) {
                if (b.checkHash()) {
                    addBlockToChain(b);
                } else {
                    LOGGER.info("[!] Received block that does not pass hash check. Not adding to block chain.");
                }
            }
        } else {
            LOGGER.info("[EE] Received ill formated blocks");
        }
        return;
    }

    private void getBlockMsgHandler(IncomingMessage message)
        throws GeneralSecurityException, InterruptedException, IOException {
        DataInputStream input =
            new DataInputStream(new ByteArrayInputStream(message.payload));
        ShaTwoFiftySix lastHash = ShaTwoFiftySix.deserialize(input);
        int ancestToGet = input.readInt();
        byte[] payload = Block.serializeBlocks(miningBundle.getBlockChain()
                                             .getAncestorsStartingAt(lastHash, ancestToGet));
        OutgoingMessage returnMsg = new OutgoingMessage(Message.BLOCK, payload);
        message.respond(returnMsg);
    }

    private void getHeadMsgHandler(IncomingMessage message)
        throws GeneralSecurityException, InterruptedException, IOException {
        Block head = miningBundle.getBlockChain().getCurrentHead();
        byte[] payload = head.getShaTwoFiftySix().copyOfHash();
        OutgoingMessage returnMsg = new OutgoingMessage(Message.HEAD, payload);
        message.respond(returnMsg);
    }

    private void headMsgHandler(IncomingMessage message)
        throws GeneralSecurityException, InterruptedException, IOException {
        DataInputStream in =
            new DataInputStream(new ByteArrayInputStream(message.payload));
        ShaTwoFiftySix headHash = ShaTwoFiftySix.deserialize(in);
        Optional<Block> hd =
            miningBundle.getBlockChain().getBlockWithHash(headHash);
        byte[] payload = Message.getBlockPayload(headHash, BLOCKS_TO_GET);
        if (!hd.isPresent()) {
            OutgoingMessage returnMsg =
                new OutgoingMessage(Message.GET_BLOCK, payload);
            message.respond(returnMsg);
        }
    }
}

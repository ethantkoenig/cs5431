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

/**
 * The MessageHandler class manages internal state and handles incoming messages
 */
public class MessageHandler {
    private static final Logger LOGGER = Logger.getLogger(MessageHandler.class.getName());

    private MiningBundle bundle;
    private BlockingQueue<OutgoingMessage> broadcastQueue;

    // The block currently being mined by the mining thread
    private Block currentHashingBlock;

    // The incomplete block to add incoming transactions to
    private Block currentAddToBlock;
    private MinerThread minerThread;
    private LinkedList<Block> miningQueue = new LinkedList<>();
    private FixedSizeSet<IncomingMessage> recentTransactionsReceived = new FixedSizeSet<>();

    public MessageHandler(BlockingQueue<OutgoingMessage> broadcast, MiningBundle miningBundle) {
        this.bundle = miningBundle;
        this.broadcastQueue = broadcast;
    }

    /**
     * Takes in an Incoming message and Transaction, adds the transactions to
     * our set of received transactions, and adds it to the block we are mining
     *
     * @param msg The incoming mesg that we recievd
     * @param tx  The transaction object that has been deserialized from msg and
     *            is to be added to the block we are working on
     */
    public void txMsgHandler(IncomingMessage msg, Transaction tx)
            throws GeneralSecurityException, InterruptedException, IOException {
        if (!recentTransactionsReceived.contains(msg)) {
            LOGGER.info("[!] New transaction, so I am broadcasting to all other miners.");
            broadcastQueue.put(new OutgoingMessage(msg.type, msg.payload));
        } else {
            return;
        }
        recentTransactionsReceived.add(msg);
        addTransactionToBlock(tx);
    }

    /**
     * @return whether {@code block} was successfully handled
     */
    public boolean blockHandler(Block block) {
        if (!block.isGenesisBlock() &&
                !bundle.getBlockChain().containsBlockWithHash(block.previousBlockHash)) {
            return false;
        }
        addBlockToChain(block);
        return true;
    }

    /**
     * Responds to a sender with a BLOCK message contain the {@code ancestToGet}
     * ancestors of block with hash {@code lastHash}
     *
     * @param message     IncomingMessage to respond to
     * @param ancestToGet int of how many ancestors to return of {@code lastHash}
     * @param lastHash    Shatwofiftysix of the Block we want ancestors of
     */
    public void getBlockMsgHandler(IncomingMessage message, int ancestToGet,
                                   ShaTwoFiftySix lastHash) throws IOException {
        byte[] payload
                = Block.serializeBlocks(bundle.getBlockChain()
                .getAncestorsStartingAt(lastHash, ancestToGet));
        OutgoingMessage returnMsg = new OutgoingMessage(Message.BLOCK, payload);
        message.respond(returnMsg);
    }

    private void startMiningThread() {
        if (miningQueue.isEmpty()) return;

        Block block = miningQueue.removeLast();
        // If there is no current miner thread then start a new one.
        if (minerThread == null || !minerThread.isAlive()) {
            currentHashingBlock = block;
            block.addReward(bundle.getKeyPair().getPublic());
            minerThread = new MinerThread(block, broadcastQueue);
            minerThread.start();
        }
    }

    private void addBlockToChain(Block block) {
        if (currentAddToBlock == null) {
            if (block.verifyGenesis(bundle.privilegedKey)) {
                LOGGER.info("Received the genesis block");
                LOGGER.info(String.format("Genesis hash: %s", block.getShaTwoFiftySix().toString()));
                if (!bundle.getBlockChain().insertBlock(block)) {
                    return;
                }
                currentHashingBlock = bundle.getBlockChain().getCurrentHead();
                LOGGER.info("[!] Creating block to put transaction on.");
                ShaTwoFiftySix previousBlockHash = bundle.getBlockChain().getCurrentHead().getShaTwoFiftySix();
                currentAddToBlock = Block.empty(previousBlockHash);
                bundle.getUnspentTransactions().put(block.getShaTwoFiftySix(), 0, block.reward);
            } else {
                LOGGER.info("Received invalid genesis block");
            }
            return;
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
        bundle.getBlockChain().insertBlock(block);
        bundle.getUnspentTransactions().put(block.getShaTwoFiftySix(), 0, block.reward);

        startMiningThread();
    }

    private void addTransactionToBlock(Transaction transaction)
            throws GeneralSecurityException, IOException {
        if (currentAddToBlock == null) {
            if (bundle.getBlockChain().getCurrentHead() == null) {
                LOGGER.warning("Received transaction before genesis block received");
                return;
            }
            LOGGER.info("[!] Creating block to put transaction on.");
            ShaTwoFiftySix previousBlockHash
                    = bundle.getBlockChain().getCurrentHead().getShaTwoFiftySix();
            currentAddToBlock = Block.empty(previousBlockHash);
        }

        //verify transaction
        LOGGER.info("[!] Verifying transaction.");
        UnspentTransactions copy = bundle.getUnspentTransactions().copy();
        if (transaction.verify(copy)) {
            bundle.setUnspentTransactions(copy);
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
}

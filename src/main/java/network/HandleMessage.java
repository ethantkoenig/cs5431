package network;
import block.Block;
import block.BlockChain;
import transaction.Transaction;
import block.UnspentTransactions;
import utils.ShaTwoFiftySix;
import utils.Pair;

import java.util.Stack;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.BlockingQueue;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.List;
/**
 * The network.HandleMessage class is in charge of responding to valid messages
 * that are passed to it.
 */
public class HandleMessage {
    private MiningBundle bundle;
    private BlockingQueue<OutgoingMessage> broadcastQ;

    // The block currently being mined by the mining thread
    private Block currentHashingBlock;

    // The incomplete block to add incoming transactions to
    private Block currentAddToBlock;
    private MinerThread minerThread;
    private LinkedList<Block> miningQueue;
    private FixedSizeSet<IncomingMessage> recentTxsRecvd;
    private Logger LOGGER;

    public HandleMessage(BlockingQueue<OutgoingMessage> broadcast, MiningBundle miningBundle,
                         Logger log) {
        this.bundle = miningBundle;
        this.miningQueue = new LinkedList<>();
        this.recentTxsRecvd = new FixedSizeSet<>();
        this.broadcastQ = broadcast;
        this.LOGGER = log;
    }

    /**
     * Takes in an Incoming message and Transaction, adds the transactions to
     * our set of received transactions, and adds it to the block we are mining
     * @param msg The incoming mesg that we recievd
     * @param txx The transaction object that has been deserialized from msg and
     *   is to be added to the block we are working on
     */
    public void txMsgHandler(IncomingMessage msg, Transaction tx)
        throws GeneralSecurityException, InterruptedException, IOException {
        if (!recentTxsRecvd.contains(msg)) {
            LOGGER.info("[!] New transaction, so I am broadcasting to all other miners.");
            broadcastQ.put(new OutgoingMessage(msg.type, msg.payload));
        } else {
            return;
        }
        recentTxsRecvd.add(msg);
        addTransactionToBlock(tx);
        return;
    }

    /**
     * Takes in an Array of Blocks, tries to add them to our blockchain, if they
     * fail to be added to the blockchain we return them.
     * @param blocks An array of bocks
     * @return A List of rejected {@code Block}s
     */
    public List<Block> blockMsgHandler(Block[] blocks) {
        ArrayList<Block> rejectedBlocks = new ArrayList<>();
        for (Block b : blocks) {
            if (!addBlockToChain(b)) {
                rejectedBlocks.add(b);
            }
        }
        return rejectedBlocks;
    }

    /**
     * Responds to a sender with a BLOCK message contain the {@code ancestToGet}
     * ancestors of block with hash {@code lastHash}
     * @param message IncomingMessage to respond to
     * @param ancestToGet int of how many ancestors to return of {@code lastHash}
     * @param lasthash Shatwofiftysix of the Block we want ancestors of
     */
    public void getBlockMsgHandler(IncomingMessage message, int ancestToGet,
                                   ShaTwoFiftySix lastHash) throws IOException {
        byte[] payload
            = Block.serializeBlocks(bundle.getBlockChain()
                                    .getAncestorsStartingAt(lastHash, ancestToGet));
        OutgoingMessage returnMsg = new OutgoingMessage(Message.BLOCK, payload);
        message.respond(returnMsg);
    }

    /**
     * Responds to a given GET_HEAD message
     * @param message IncomingMessage asking for head
     */
    public void getHeadMsgHandler(IncomingMessage message)
        throws GeneralSecurityException, InterruptedException, IOException {
        Block head = bundle.getBlockChain().getCurrentHead();
        byte[] payload = head.getShaTwoFiftySix().copyOfHash();
        OutgoingMessage returnMsg = new OutgoingMessage(Message.HEAD, payload);
        message.respond(returnMsg);
    }

    /**
     * Given the sender of a HEAD message, we send back a GET_BLOCK message
     * asking for {@code BLOCKS_TO_GET} ancestors of {@code headHash}
     * @param message IncomingMessage to resond to who gave us HEAD
     * @param headHash Shatwofiftysix of block to get ancestors of
     */
    public static void headMsgHandler(IncomingMessage message, ShaTwoFiftySix headHash)
        throws GeneralSecurityException, InterruptedException, IOException {
        headMsgHandler(message, headHash, Message.BLOCKS_TO_GET);
    }

    /**
     * Given the sender of a HEAD message, we send back a GET_BLOCK message
     * asking for {@code numRequestblocks} ancestors of {@code headHash}
     * @param message IncomingMessage to resond to who gave us HEAD
     * @param headHash Shatwofiftysix of block to get ancestors of
     * @param numrequestblocks int of how many ancestors to get
     */
    public static void headMsgHandler(IncomingMessage message, ShaTwoFiftySix headHash,
                                      int numRequestBlocks) throws IOException {
        byte[] payload = Message.getBlockPayload(headHash, numRequestBlocks);
        OutgoingMessage returnMsg =
            new OutgoingMessage(Message.GET_BLOCK, payload);
        message.respond(returnMsg);

    }

    /**
     * Attempts to add the given Stack of Blocks to the blockchain, returns true
     * if all blocks were able to be added, false if not
     * @param blocks Stack of Blocks to be added to the blockchain
     * @return boolean true if all were sucessful, false if not
     */
    public boolean addStackToChain(Stack<Block> blocks) {
        if (blocks.empty()) {
            return true;
        } else {
            return true && addBlockToChain(blocks.pop())
                && addStackToChain(blocks);
        }
    }

    /**
     * Asks the sender of the message for the current head of their blockchain
     * @param msg IncomingMessage to respond to
     */
    public static void askForHead(IncomingMessage msg) throws IOException {
        msg.respond(new OutgoingMessage(Message.GET_HEAD, new byte[0]));
    }

    private void startMiningThread() {
        if (miningQueue.isEmpty()) return;

        Block block = miningQueue.removeLast();
        // If there is no current miner thread then start a new one.
        if (minerThread == null || !minerThread.isAlive()) {
            currentHashingBlock = block;
            block.addReward(bundle.getKeyPair().getPublic());
            minerThread = new MinerThread(block, broadcastQ);
            minerThread.start();
        }
    }

    private boolean addBlockToChain(Block block) {
        boolean toReturn = false;
        if (currentAddToBlock == null) {
            if (block.verifyGenesis(bundle.privilegedKey)) {
                LOGGER.info("Received the genesis block");
                LOGGER.info(String.format("Genesis hash: %s", block.getShaTwoFiftySix().toString()));
                toReturn = bundle.getBlockChain().insertBlock(block);
                currentHashingBlock = bundle.getBlockChain().getCurrentHead();
                LOGGER.info("[!] Creating block to put transaction on.");
                ShaTwoFiftySix previousBlockHash = bundle.getBlockChain().getCurrentHead().getShaTwoFiftySix();
                currentAddToBlock = Block.empty(previousBlockHash);
                bundle.getUnspentTransactions().put(block.getShaTwoFiftySix(), 0, block.reward);
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
        toReturn = bundle.getBlockChain().insertBlock(block);
        bundle.getUnspentTransactions().put(block.getShaTwoFiftySix(), 0, block.reward);

        startMiningThread();
        return toReturn;
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

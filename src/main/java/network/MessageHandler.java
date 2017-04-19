package network;

import block.Block;
import block.UnspentTransactions;
import crypto.ECDSAPublicKey;
import transaction.Transaction;
import transaction.TxOut;
import utils.ByteUtil;
import utils.CanBeSerialized;
import utils.Pair;
import utils.ShaTwoFiftySix;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

/**
 * The MessageHandler class manages internal state and handles incoming messages
 */
public class MessageHandler {
    private static final Logger LOGGER = Logger.getLogger(MessageHandler.class.getName());

    private final MiningBundle bundle;
    private final BlockingQueue<OutgoingMessage> broadcastQueue;

    // list to add incoming transactions to
    private final List<Transaction> unminedTransactions = new ArrayList<>();
    private MinerThread minerThread;
    private LinkedList<Block> miningQueue = new LinkedList<>();
    private FixedSizeSet<IncomingMessage> recentTransactionsReceived = new FixedSizeSet<>();

    private boolean isMining;

    public MessageHandler(BlockingQueue<OutgoingMessage> broadcast,
                          MiningBundle miningBundle,
                          boolean isMining) {
        this.bundle = miningBundle;
        this.broadcastQueue = broadcast;
        this.isMining = isMining;
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
            throws InterruptedException, IOException {
        if (!recentTransactionsReceived.contains(msg)) {
            LOGGER.info("[!] New transaction, so I am broadcasting to all other miners.");
            broadcastQueue.put(new OutgoingMessage(msg.type, msg.payload));
        } else {
            return;
        }
        recentTransactionsReceived.add(msg);
        addTransaction(tx);
    }

    /**
     * @return whether {@code block} was successfully handled
     */
    public boolean blockHandler(Block block) {
        if (!block.isGenesisBlock() &&
                !bundle.getBlockChain().containsBlockWithHash(block.previousBlockHash)) {
            return false;
        }
        try {
            addBlockToChain(block);
        } catch (GeneralSecurityException | IOException e) {
            LOGGER.severe(e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Responds to a sender with a BLOCK message contain the {@code ancestToGet}
     * ancestors of block with hash {@code lastHash}
     *
     * @param message IncomingMessage to respond to
     * @param request Request for blocks
     */
    public void getBlockMsgHandler(IncomingMessage message, GetBlocksRequest request) throws IOException {
        List<Block> ancestors = bundle.getBlockChain()
                .getAncestorsStartingAt(request.hash, request.numBlocksRequested);
        byte[] payload = ByteUtil.asByteArray(out -> CanBeSerialized.serializeList(out, ancestors));
        OutgoingMessage returnMsg = new OutgoingMessage(Message.BLOCK, payload);
        message.respond(returnMsg);
    }

    private void startMiningThread() {
        if (miningQueue.isEmpty()) return;

        Block block = miningQueue.removeLast();
        // If there is no current miner thread then start a new one.
        if (minerThread == null || !minerThread.isAlive()) {
            block.addReward(bundle.getKeyPair().publicKey);
            minerThread = new MinerThread(block, broadcastQueue);
            minerThread.start();
        }
    }

    /**
     * Responds to a sender with a `FUNDS` message containing the map from `ECDSAPublicKey`s to
     * total funds owned by those keys.
     *
     * @param message The message from the sender. Must have a non-null `responder`
     * @param request The `GET_FUNDS` request to respond to
     * @throws IOException
     */
    public void getFundsMsgHandler(IncomingMessage message, GetFundsRequest request)
        throws IOException {
        // For each key, get the available funds from unspend Txs
        List<ECDSAPublicKey> keys = request.requestedKeys;
        HashMap<ECDSAPublicKey, Long> funds = new HashMap<>();
        for (Map.Entry<Pair<ShaTwoFiftySix, Integer>, TxOut> entry : bundle.getUnspentTransactions()) {
            for (ECDSAPublicKey key : keys) {
                if (key.equals(entry.getValue().ownerPubKey)) {
                    funds.compute(key, (k,v) -> ((v == null) ? 0L : v) + entry.getValue().value);
                }
            }
        }
        for (ECDSAPublicKey key: keys) {
            funds.putIfAbsent(key, 0L);
        }
        // Generate a Funds message and return it to sender
        GetFundsResponse toReturn = new GetFundsResponse(funds);
        byte[] payload = ByteUtil.asByteArray(toReturn::serialize);
        message.respond(new OutgoingMessage(Message.FUNDS, payload));
    }

    /**
     * Responds to a sender with a `UTX_WITH_KEYS` message containing the unsigned `Transaction`
     * as well as the list of public keys corresponding to the inputs.
     *
     * @param message The message from the sender. Must have a non-null `responder`
     * @param request The `GET_UTX_WITH_KEYS` request to respond to
     * @throws IOException
     */
    public void getUTXWithKeysMsgHandler(IncomingMessage message, GetUTXWithKeysRequest request)
        throws IOException {
        Optional<Pair<List<ECDSAPublicKey>,Transaction>> result =
                bundle.getUnspentTransactions()
                        .buildUnsignedTransaction(
                                request.keys, request.changeKey,
                                request.destination, request.amount);

        GetUTXWithKeysResponse toReturn = result
                .map(p -> GetUTXWithKeysResponse.success(p.getLeft(),p.getRight()))
                .orElseGet(GetUTXWithKeysResponse::failure);
        byte[] payload = ByteUtil.asByteArray(toReturn::serialize);
        message.respond(new OutgoingMessage(Message.UTX_WITH_KEYS, payload));
    }

    private void addBlockToChain(Block block) throws GeneralSecurityException, IOException {
        if (bundle.getBlockChain().getCurrentHead() == null) {
            if (block.verifyGenesis(bundle.privilegedKey)) {
                LOGGER.info("Received the genesis block");
                LOGGER.info(String.format("Genesis hash: %s", block.getShaTwoFiftySix().toString()));
                if (!bundle.getBlockChain().insertBlock(block)) {
                    LOGGER.severe("Unable to add genesis block");
                    return;
                }
                bundle.getUnspentTransactions().put(block.getShaTwoFiftySix(), 0, block.reward);
            } else {
                LOGGER.warning("Received invalid genesis block");
            }
            return;
        }
        Optional<UnspentTransactions> verifiedUnspentTransactions = bundle.getBlockChain().verifyBlock(block);
        if (!verifiedUnspentTransactions.isPresent()) {
            LOGGER.warning("Received invalid block");
            return;
        }

        LOGGER.info(String.format("Received valid block: hash=%s", block.getShaTwoFiftySix()));

        // interrupt the mining thread
        if (isMining) {
            if (minerThread != null && minerThread.isAlive()) {
                LOGGER.info("[-] Received block. Stopping current mining thread.");
                minerThread.stopMining();
            }
        }

        // Add block to chain
        LOGGER.info("[+] Adding completed block to block chain");
        bundle.getBlockChain().insertBlock(block);
        bundle.setUnspentTransactions(verifiedUnspentTransactions.get());
        if (isMining) {
            unminedTransactions.clear();
            startMiningThread();
        }
    }

    private void addTransaction(Transaction transaction)
            throws IOException {
        if (bundle.getBlockChain().getCurrentHead() == null) {
            LOGGER.warning("Received transaction before genesis block received");
            return;
        } else if (unminedTransactions.size() == Block.NUM_TRANSACTIONS_PER_BLOCK) {
            LOGGER.warning("Dropping incoming transaction, resend when a new block has been mined");
            return;
        }

        //verify transaction
        LOGGER.info("[!] Verifying transaction.");
        UnspentTransactions copy = bundle.getUnspentTransactions().copy();
        if (!transaction.verify(copy)) {
            LOGGER.warning("The received transaction was not verified! Not adding to block.");
            return;
        }
        bundle.setUnspentTransactions(copy);
        LOGGER.info("[!] Transaction verified.");
        LOGGER.info(transaction.toString());
        unminedTransactions.add(transaction);
        if (unminedTransactions.size() == Block.NUM_TRANSACTIONS_PER_BLOCK) {
            // currentAddToBlock is full, so start mining it
            LOGGER.info("[+] Starting mining thread");

            Block blockToMine = Block.empty(bundle.getBlockChain().getCurrentHead().getShaTwoFiftySix());
            for (Transaction t : unminedTransactions) {
                blockToMine.addTransaction(t);
            }
            miningQueue.addFirst(blockToMine);
            startMiningThread();
            addTransaction(transaction);
        }
    }
}

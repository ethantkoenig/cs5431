package network;

import block.Block;
import block.UnspentTransactions;
import crypto.ECDSAPublicKey;
import message.IncomingMessage;
import message.Message;
import message.OutgoingMessage;
import message.payloads.*;
import transaction.Transaction;
import transaction.TxIn;
import transaction.TxOut;
import utils.ByteUtil;
import utils.Log;
import utils.Pair;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

/**
 * The MessageHandler class manages internal state and handles incoming messages
 */
public class MessageHandler {
    private final Log LOGGER;
    private final String name;

    private final MiningBundle bundle;
    private final BlockingQueue<OutgoingMessage> broadcastQueue;

    // list to add incoming transactions to
    private final List<Transaction> unminedTransactions = new ArrayList<>();
    private MinerThread minerThread;
    private LinkedList<Block> miningQueue = new LinkedList<>();
    private FixedSizeSet<IncomingMessage> recentTransactionsReceived = new FixedSizeSet<>();
    private FixedSizeSet<IncomingMessage> recentBlocksReceived = new FixedSizeSet<>();

    private boolean isMining;

    public MessageHandler(String name,
                          BlockingQueue<OutgoingMessage> broadcast,
                          MiningBundle miningBundle,
                          boolean isMining) {
        LOGGER = Log.named("MessageHandler " + name);
        this.name = name;
        this.bundle = miningBundle;
        this.broadcastQueue = broadcast;
        this.isMining = isMining;
    }

    /**
     * Takes in an Incoming message and Transaction, adds the transactions to
     * our set of received transactions, and adds it to the block we are mining
     *
     * @param msg The incoming message that we received
     * @param tx  The transaction object that has been deserialized from msg and
     *            is to be added to the block we are working on
     */
    public void txMsgHandler(IncomingMessage msg, Transaction tx)
            throws InterruptedException, IOException {
        if (recentTransactionsReceived.contains(msg)) {
            return;
        }
        LOGGER.info("[!] New transaction, so I am broadcasting to all other miners.");
        broadcastQueue.put(new OutgoingMessage(msg.type, msg.payload));
        recentTransactionsReceived.add(msg);
        addTransaction(tx);
    }

    public boolean checkRecentBlock(IncomingMessage msg) {
        return recentBlocksReceived.contains(msg);
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
        } catch (IOException e) {
            LOGGER.severe(e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Rebroadcasts new blocks to other all other nodes.
     */
    public void blockBroadcaster(IncomingMessage msg) throws InterruptedException {
        if (recentBlocksReceived.contains(msg)) {
            return;
        }
        LOGGER.info("New block, broadcasting to all other miners.");
        broadcastQueue.put(new OutgoingMessage(msg.type, msg.payload));
        LOGGER.info("New block put onto broadcastQueue");
        recentBlocksReceived.add(msg);
    }

    /**
     * Responds to a sender with a BLOCKS message contain the {@code ancestToGet}
     * ancestors of block with hash {@code lastHash}
     *
     * @param message IncomingMessage to respond to
     * @param request Request for blocks
     */
    public void getBlockMsgHandler(IncomingMessage message, GetBlocksRequestPayload request) throws IOException {
        List<Block> ancestors = bundle.getBlockChain()
                .getAncestorsStartingAt(request.hash, request.numBlocksRequested);
        message.respond(new BlocksPayload(ancestors).toMessage());
    }

    private void startMiningThread() {
        if (miningQueue.isEmpty()) return;

        Block block = miningQueue.removeLast();
        // If there is no current miner thread then start a new one.
        if (minerThread == null || !minerThread.isAlive()) {
            block.addReward(bundle.getKeyPair().publicKey);
            minerThread = new MinerThread(name, block, broadcastQueue);
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
    public void getFundsMsgHandler(IncomingMessage message, GetFundsRequestPayload request)
            throws IOException {
        Map<ECDSAPublicKey, Long> funds = request.requestedKeys.stream()
                .collect(Collectors.toMap(key -> key, key -> 0L));
        for (Map.Entry<TxIn, TxOut> entry : bundle.getUnspentTransactions()) {
            ECDSAPublicKey key = entry.getValue().ownerPubKey;
            if (funds.containsKey(key)) {
                funds.put(key, funds.get(key) + entry.getValue().value);
            }
        }
        message.respond(new GetFundsResponsePayload(funds).toMessage());
    }

    /**
     * Responds to a sender with a `UTX_WITH_KEYS` message containing the unsigned `Transaction`
     * as well as the list of public keys corresponding to the inputs.
     *
     * @param message The message from the sender. Must have a non-null `responder`
     * @param request The `GET_UTX_WITH_KEYS` request to respond to
     * @throws IOException
     */
    public void getUTXWithKeysMsgHandler(IncomingMessage message, GetUTXWithKeysRequestPayload request)
            throws IOException {
        Optional<Pair<List<ECDSAPublicKey>, Transaction>> result =
                bundle.getUnspentTransactions()
                        .buildUnsignedTransaction(
                                request.keys, request.changeKey,
                                request.destination, request.amount);

        GetUTXWithKeysResponsePayload toReturn = result
                .map(p -> GetUTXWithKeysResponsePayload.success(p.getLeft(), p.getRight()))
                .orElseGet(GetUTXWithKeysResponsePayload::failure);
        byte[] payload = ByteUtil.asByteArray(toReturn::serialize);
        message.respond(new OutgoingMessage(Message.UTX_WITH_KEYS, payload));
    }

    private void addBlockToChain(Block block) throws IOException {
        if (block.isGenesisBlock()) {
            if (bundle.getBlockChain().getCurrentHead() != null) {
                LOGGER.warning("Received a genesis block after having already received one");
            } else if (block.verifyGenesis(bundle.privilegedKey)) {
                LOGGER.info("Received the genesis block");
                LOGGER.info("Genesis hash: %s", block.getShaTwoFiftySix());
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
        Optional<UnspentTransactions> verifiedUnspentTransactions = bundle.getBlockChain().verifyNonGenesisBlock(block);
        if (!verifiedUnspentTransactions.isPresent()) {
            LOGGER.warning("Received invalid block");
            return;
        }

        LOGGER.info("Received valid block: hash=%s", block.getShaTwoFiftySix());

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
        if (bundle.getBlockChain().getCurrentHead().equals(block)) {
            bundle.setUnspentTransactions(verifiedUnspentTransactions.get());
        }
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
        } else if (unminedTransactions.size() == Block.NUM_TRANSACTIONS_PER_BLOCK && isMining) {
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
        if (isMining) {
            unminedTransactions.add(transaction);
        }
        if (isMining && unminedTransactions.size() == Block.NUM_TRANSACTIONS_PER_BLOCK) {
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

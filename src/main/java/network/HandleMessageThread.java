package network;

import block.Block;
import block.BlockChain;
import transaction.Transaction;
import utils.ByteUtil;
import utils.DeserializationException;
import utils.Deserializer;
import utils.ShaTwoFiftySix;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;


/**
 * The network.HandleMessageThread is a background thread ran by the instantiated Node class
 * in order to process incoming messages from all connected nodes.
 *
 * @version 1.0, Feb 22 2017
 */
public class HandleMessageThread extends Thread {
    // Blocks to request when we are behind other nodes
    private static final Logger LOGGER = Logger.getLogger(HandleMessageThread.class.getName());

    private BlockingQueue<IncomingMessage> messageQueue;

    private MessageHandler handler;

    private MiningBundle bundle;

    private final OrphanedBlocks orphanedBlocks = new OrphanedBlocks();

    // Needs reference to parent in order to call Node.broadcast()
    public HandleMessageThread(BlockingQueue<IncomingMessage> messageQueue,
                               BlockingQueue<OutgoingMessage> broadcastQueue,
                               MiningBundle bundle) {
        this.messageQueue = messageQueue;
        this.bundle = bundle;
        this.handler = new MessageHandler(broadcastQueue, bundle);
    }

    /**
     * The run() function is ran when the thread is started. We pull off of the synchronized blocking messageQueue
     * whenever there is a message to be pulled. We then consume this message appropriately.
     */
    @Override
    public void run() {
        while (true) {
            handleNextMessage();
        }
    }

    private void handleNextMessage() {
        try {
            handleMessageRecklessly(messageQueue.take());
        } catch (DeserializationException | InterruptedException | IOException e) {
            LOGGER.severe("Error handling message: " + e.getMessage());
        }
    }

    private void handleMessageRecklessly(IncomingMessage message)
            throws DeserializationException, InterruptedException, IOException {
        switch (message.type) {
            case Message.TRANSACTION:
                LOGGER.info("[!] Received transaction!");
                Transaction transaction = Transaction.DESERIALIZER.deserialize(message.payload);
                handler.txMsgHandler(message, transaction);
                break;
            case Message.BLOCK:
                Block[] blocks = Deserializer.deserializeList(message.payload, Block.DESERIALIZER)
                        .toArray(new Block[0]);
                boolean added = false;
                // TODO check that blocks are in "correct" order (parent, child, grandchild, ...)
                for (int i = blocks.length - 1; i >= 0; i--) {
                    Block block = blocks[i];
                    if (handler.blockHandler(block)) {
                        for (Block descendant : orphanedBlocks.popDescendantsOf(block.getShaTwoFiftySix())) {
                            if (!handler.blockHandler(descendant)) {
                                LOGGER.severe("Unexpectedly unable to handle block");
                            }
                        }
                        added = true;
                        break;
                    } else {
                        orphanedBlocks.add(block);
                    }
                }
                if (!added) {
                    ShaTwoFiftySix hash = blocks[0].getShaTwoFiftySix();
                    GetBlocksRequest request = new GetBlocksRequest(hash, 10);
                    byte[] payload = ByteUtil.asByteArray(request::serialize);
                    message.respond(new OutgoingMessage(Message.GET_BLOCK, payload));
                }
                break;
            case Message.GET_BLOCK:
                handleGetBlockRequest(message);
                break;
            default:
                LOGGER.severe(String.format("Unexpected message type: %d", message.type));
        }
    }

    private void handleGetBlockRequest(IncomingMessage message)
            throws DeserializationException, IOException {
        GetBlocksRequest request = GetBlocksRequest.DESERIALIZER.deserialize(message.payload);
        BlockChain chain = bundle.getBlockChain();
        if (request.numBlocksRequested <= 0 ||
                request.numBlocksRequested >= Message.MAX_BLOCKS_TO_GET) {
            String msg = String.format("GET_BLOCK request, invalid number of requested blocks: %d",
                    request.numBlocksRequested);
            LOGGER.info(msg);
            return;
        } else if (!chain.containsBlockWithHash(request.hash)) {
            LOGGER.info("GET_BLOCK message received with unknown hash");
            return;
        }
        handler.getBlockMsgHandler(message, request);
    }
}

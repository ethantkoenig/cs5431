package network;

import block.Block;
import block.BlockChain;
import transaction.Transaction;
import utils.ShaTwoFiftySix;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;
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
        try {
            IncomingMessage message;
            while ((message = messageQueue.take()) != null) {
                switch (message.type) {
                    case Message.TRANSACTION:
                        LOGGER.info("[!] Received transaction!");
                        Transaction transaction =
                                Transaction.deserialize(message.payload);
                        handler.txMsgHandler(message, transaction);
                        break;
                    case Message.BLOCK:
                        Optional<Block[]> opt = Block.deserializeBlocks(message.payload);
                        if (!opt.isPresent()) {
                            LOGGER.info("(WW) received misformatted BLOCK message or block that did not pass hashcheck.");
                            break;
                        }
                        Block[] blocks = opt.get();
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
                            message.respond(new OutgoingMessage(
                                    Message.GET_BLOCK,
                                    Message.getBlockPayload(blocks[0].getShaTwoFiftySix(), 10)
                            ));
                        }
                        break;
                    case Message.GET_BLOCK:
                        handleGetBlockRequest(message);
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

    private void handleGetBlockRequest(IncomingMessage message)
            throws IOException {
        DataInputStream input =
                new DataInputStream(new ByteArrayInputStream(message.payload));
        // TODO move deserialization elsewhere
        ShaTwoFiftySix lastHash = ShaTwoFiftySix.deserialize(input);
        int aToGet = input.readInt();
        BlockChain chain = bundle.getBlockChain();
        if (aToGet <= 0 || aToGet >= Message.MAX_BLOCKS_TO_GET) {
            LOGGER.info("GET_BLOCK message received with invalid number of blocks to get");
            return;
        } else if (!chain.containsBlockWithHash(lastHash)) {
            LOGGER.info("GET_BLOCK message received with unknown hash");
            return;
        }
        handler.getBlockMsgHandler(message, aToGet, lastHash);
    }
}

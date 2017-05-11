package network;

import block.Block;
import block.BlockChain;
import message.IncomingMessage;
import message.Message;
import message.OutgoingMessage;
import message.payloads.*;
import transaction.Transaction;
import utils.Config;
import utils.DeserializationException;
import utils.Deserializer;
import utils.Log;
import utils.ShaTwoFiftySix;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;


/**
 * The network.HandleMessageThread is a background thread ran by the instantiated Node class
 * in order to process incoming messages from all connected nodes.
 *
 * @version 1.0, Feb 22 2017
 */
public class HandleMessageThread extends Thread {
    private static final Log LOGGER = Log.forClass(HandleMessageThread.class);

    private BlockingQueue<IncomingMessage> messageQueue;

    private MessageHandler handler;

    private MiningBundle bundle;

    // Blocks to request when we are behind other nodes
    private final OrphanedBlocks orphanedBlocks = new OrphanedBlocks();

    // Needs reference to parent in order to call Node.broadcast()
    public HandleMessageThread(BlockingQueue<IncomingMessage> messageQueue,
                               BlockingQueue<OutgoingMessage> broadcastQueue,
                               MiningBundle bundle,
                               boolean isMining) {
        this.messageQueue = messageQueue;
        this.bundle = bundle;
        this.handler = new MessageHandler(broadcastQueue, bundle, isMining);
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
            case Message.BLOCKS:
                List<Block> blocks = Deserializer.deserializeList(message.payload, Block.DESERIALIZER);
                if (blocks.isEmpty()) {
                    LOGGER.warning("Received empty blocks message");
                    break;
                }
                boolean added = false;

                // TODO check that blocks are in "correct" order (grandchild, child, parent ...)
                for (Block block : blocks) {
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
                    ShaTwoFiftySix hash = blocks.get(blocks.size() - 1).getShaTwoFiftySix();
                    message.respond(new GetBlocksRequestPayload(hash, Message.MAX_BLOCKS_TO_GET).toMessage());
                }
                break;
            case Message.GET_BLOCKS:
                handleGetBlockRequest(message);
                break;
            case Message.GET_FUNDS:
                GetFundsRequestPayload request
                        = GetFundsRequestPayload.DESERIALIZER.deserialize(message.payload);
                handler.getFundsMsgHandler(message, request);
                break;
            case Message.GET_UTX_WITH_KEYS:
                GetUTXWithKeysRequestPayload utxRequest =
                        GetUTXWithKeysRequestPayload.DESERIALIZER.deserialize(message.payload);
                handler.getUTXWithKeysMsgHandler(message, utxRequest);
                break;
            case Message.PING:
                int pingNumber = PingPayload.DESERIALIZER.deserialize(message.payload).pingNumber;
                message.respond(new PongPayload(pingNumber).toMessage());
                break;
            default:
                LOGGER.severe("Unexpected message type: %d", message.type);
        }
    }

    private void handleGetBlockRequest(IncomingMessage message)
            throws DeserializationException, IOException {
        GetBlocksRequestPayload request = GetBlocksRequestPayload.DESERIALIZER.deserialize(message.payload);
        BlockChain chain = bundle.getBlockChain();
        if (request.numBlocksRequested <= 0 ||
                request.numBlocksRequested > Message.MAX_BLOCKS_TO_GET) {
            LOGGER.info("GET_BLOCKS request, invalid number of requested blocks: %d", request.numBlocksRequested);
            return;
        } else if (!chain.containsBlockWithHash(request.hash)) {
            LOGGER.info("GET_BLOCKS message received with unknown hash: %s", request.hash);
            return;
        }
        handler.getBlockMsgHandler(message, request);
    }
}

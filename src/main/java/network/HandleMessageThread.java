package network;

import block.Block;
import block.BlockChain;
import block.UnspentTransactions;
import transaction.Transaction;
import utils.ShaTwoFiftySix;
import utils.Pair;

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
import java.util.Stack;
import java.util.List;


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

    private HandleMessage handler;

    private MiningBundle bundle;

    private ArrayList<Stack<Block>> blocksToAdd;

    // Needs reference to parent in order to call Node.broadcast()
    public HandleMessageThread(BlockingQueue<IncomingMessage> messageQueue, BlockingQueue<OutgoingMessage> broadcastQueue, MiningBundle bundle) {
        this.messageQueue = messageQueue;
        this.bundle = bundle;
        this.blocksToAdd = new ArrayList<Stack<Block>>();
        this.handler = new HandleMessage(broadcastQueue, bundle, LOGGER);
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
                if (!checkLen(message.payload)) {
                    LOGGER.info("(WW) Message payload too large.");
                    continue;
                }
                switch (message.type) {
                    case Message.TRANSACTION:
                        LOGGER.info("[!] Received transaction!");
                        Transaction transaction =
                            Transaction.deserialize(message.payload);
                        handler.txMsgHandler(message, transaction);
                        break;
                    case Message.BLOCK:
                        Optional<Block[]> opt = Block.deserializeBlocks(message.payload);
                        if (opt.isPresent()) {
                            List<Block> rejects = handler.blockMsgHandler(opt.get());
                            if (!rejects.isEmpty()) {
                                // Add blocks to stack
                                addToStacks(rejects);
                                handler.askForHead(message);
                            }
                            addValidStacks();
                        } else {
                            LOGGER.info("(WW) recieved ill formatted BLOCK message or block that did not pass hashcheck.");
                        }
                        break;
                    case Message.GET_BLOCK:
                        Optional<Pair<Integer, ShaTwoFiftySix>> optPar
                            = checkGetBlockArgs(message.payload);
                        if (optPar.isPresent()) {
                            handler.getBlockMsgHandler(message,
                                                       optPar.get().getLeft(),
                                                       optPar.get().getRight());
                        }
                        break;
                    case Message.GET_HEAD:
                        handler.getHeadMsgHandler(message);
                        break;
                    case Message.HEAD:
                        Optional<ShaTwoFiftySix> optl = checkHeadArgs(message.payload);
                        if (optl.isPresent()) {
                            ShaTwoFiftySix headHash = optl.get();
                            if (bundle.getBlockChain().containsBlockWithHash(headHash)) {
                                LOGGER.info("Recievd HEAD message containing hash we already have");
                            } else {
                                handler.headMsgHandler(message, headHash);
                            }
                        } else {
                            LOGGER.info("(WW) Recieved bad HEAD");
                        }

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

    private void addToStacks(List<Block> blocks) {
        for (Block b : blocks) {
            addToStacks(b);
        }
    }

    /*
     * Adds block to a stack that accepts it, if none do, creates a new stack.
     */
    private void addToStacks(Block b) {
        for (Stack<Block> s : blocksToAdd) {
            Block head = s.peek();
            if (head.previousBlockHash.equals(b.getShaTwoFiftySix())) {
                s.push(b);
                return;
            }
        }
        Stack<Block> newStack = new Stack<Block>();
        newStack.push(b);
        blocksToAdd.add(newStack);
        return;
    }

    /*
     * Checks to see if any stack can be added to the blockchain, if so uses the
     * handler to add it to the blockchain and removes from blocksToAdd
     */
    private void addValidStacks() {
        ArrayList<Stack<Block>> toRemove = new ArrayList<>();
        BlockChain chain = bundle.getBlockChain();
        for (Stack<Block> s : blocksToAdd) {
            Block stackHead = s.peek();
            if (chain.containsBlockWithHash(stackHead.previousBlockHash)) {
                if (!handler.addStackToChain(s)) {
                    LOGGER.info("(!!) Invalid block in stack, block not added to blockchain and stack removed");
                }
                toRemove.add(s);
            }
        }
        blocksToAdd.removeAll(toRemove);
    }

    private static boolean checkLen(byte[] payload) {
        return payload.length < Message.MAX_PAYLOAD_LEN;
    }

    private Optional<Pair<Integer, ShaTwoFiftySix>> checkGetBlockArgs(byte[] payload)
        throws IOException {
        DataInputStream input =
            new DataInputStream(new ByteArrayInputStream(payload));
        ShaTwoFiftySix lastHash = ShaTwoFiftySix.deserialize(input);
        int aToGet = input.readInt();
        BlockChain chain = bundle.getBlockChain();
        if (aToGet <= 0) {
            LOGGER.info("(WW) GET_HEAD message recieved with invalid number of blocks to get");
            return Optional.empty();
        } else if (chain.containsBlockWithHash(lastHash)) {
            LOGGER.info("(WW) GET_HEAD message recieved with unknown hash");
            return Optional.empty();
        } else {
            return Optional.of(new Pair(aToGet, lastHash));
        }
    }

    private Optional<ShaTwoFiftySix> checkHeadArgs(byte[] payload)
        throws IOException {
        if (payload.length == ShaTwoFiftySix.HASH_SIZE_IN_BYTES) {
            DataInputStream in =
                new DataInputStream(new ByteArrayInputStream(payload));
            ShaTwoFiftySix headHash = ShaTwoFiftySix.deserialize(in);
            Optional<Block> hd =
                bundle.getBlockChain().getBlockWithHash(headHash);
        }
        return Optional.empty();
    }
}

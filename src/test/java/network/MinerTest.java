package network;


import block.Block;
import generators.model.BlockGenerator;
import org.junit.Assert;
import org.junit.Test;
import testutils.RandomizedTest;
import testutils.TestUtils;
import transaction.Transaction;
import transaction.TxIn;
import transaction.TxOut;
import utils.ByteUtil;
import utils.Config;
import utils.Deserializer;
import utils.ShaTwoFiftySix;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class MinerTest extends RandomizedTest {

    @Test
    public void testSuccessfulRun() throws Exception {
        Config.HASH_GOAL.set(1);

        KeyPair pair0 = randomKeyPair();
        KeyPair pair1 = randomKeyPair();

        MinerSimulation simulation = new MinerSimulation(10100);
        simulation.addNode(10101, pair0, pair1.getPublic());
        simulation.addNode(10102, pair1, pair1.getPublic());

        Block genesisBlock = assertSingleBlockMessage(simulation.getNextMessage());

        simulation.sendBytes(randomBytes(1024), 0); // should be ignored
        simulation.sendBytes(randomBytes(1024), 1); // should be ignored

        simulation.sendGetBlocksRequest(genesisBlock.getShaTwoFiftySix(), 1, 1);
        Block getBlockResponse = assertSingleBlockMessage(simulation.getNextMessage(true));
        Assert.assertEquals(genesisBlock, getBlockResponse);

        Transaction transaction1 = new Transaction.Builder()
                .addInput(new TxIn(genesisBlock.getShaTwoFiftySix(), 0), pair1.getPrivate())
                .addOutput(new TxOut(Block.REWARD_AMOUNT, pair0.getPublic()))
                .build();
        simulation.sendTransaction(transaction1, 0);
        Transaction deserialized = assertTransactionMessage(simulation.getNextMessage());
        TestUtils.assertEqualsWithHashCode(errorMessage, transaction1, deserialized);

        KeyPair pair2 = randomKeyPair();
        simulation.addNode(10103, pair2, pair1.getPublic());

        Thread.sleep(200); // TODO actually fix the race conditions

        Transaction transaction2 = new Transaction.Builder()
                .addInput(new TxIn(transaction1.getShaTwoFiftySix(), 0), pair0.getPrivate())
                .addOutput(new TxOut(Block.REWARD_AMOUNT, pair2.getPublic()))
                .build();
        simulation.sendTransaction(transaction2, 1);
        deserialized = assertTransactionMessage(simulation.getNextMessage());
        TestUtils.assertEqualsWithHashCode(errorMessage, transaction2, deserialized);

        Block block = assertSingleBlockMessage(simulation.getNextMessage());
        boolean minedBy0 = block.reward.ownerPubKey.equals(pair0.getPublic());
        boolean minedBy1 = block.reward.ownerPubKey.equals(pair1.getPublic());
        Assert.assertTrue(minedBy0 || minedBy1);

        Thread.sleep(200); // TODO actually fix the race conditions

        simulation.sendGetBlocksRequest(block.getShaTwoFiftySix(), 2, 2);
        while (true) {
            // may receive mined block from other node, so repeatedly check
            // for actual GET_BLOCK response
            Block[] blocks = assertBlockMessage(simulation.getNextMessage());
            if (blocks.length == 2) {
                Assert.assertArrayEquals(
                        new Block[]{block, genesisBlock},
                        blocks
                );
                break;
            }
        }
    }

    @Test
    public void testInvalidTransactionRejection() throws Exception {
        Config.HASH_GOAL.set(1);

        KeyPair pair0 = randomKeyPair();
        KeyPair pair1 = randomKeyPair();
        Block[] empty = new Block[0];

        MinerSimulation simulation = new MinerSimulation(10100);
        simulation.addNode(10101, pair0, pair1.getPublic());

        Block genesisBlock = assertSingleBlockMessage(simulation.getNextMessage());

        Transaction txId = new Transaction.Builder()
                .addInput(new TxIn(genesisBlock.getShaTwoFiftySix(), 0), pair1.getPrivate())
                .addOutput(new TxOut(Block.REWARD_AMOUNT, pair1.getPublic()))
                .build();

        Transaction txNonexistentBlock = new Transaction.Builder()
                .addInput(new TxIn(randomShaTwoFiftySix(),0), pair0.getPrivate())
                .addOutput(new TxOut(50, pair0.getPublic()))
                .build();

        Block badBlock = Block.empty(genesisBlock.getShaTwoFiftySix());
        badBlock.addTransaction(txId);
        badBlock.addTransaction(txNonexistentBlock);
        badBlock.addReward(pair1.getPublic());
        mineBlock(badBlock);

        simulation.sendBlock(badBlock, 0);
        simulation.sendGetBlocksRequest(badBlock.getShaTwoFiftySix(), 1, 0);
        Block[] blocks = assertBlockMessage(simulation.getNextMessage());
        Assert.assertArrayEquals(empty, blocks);


        Transaction txBadIndex = new Transaction.Builder()
                .addInput(new TxIn(genesisBlock.getShaTwoFiftySix(), 1), pair1.getPrivate())
                .addOutput(new TxOut(Block.REWARD_AMOUNT, pair1.getPublic()))
                .build();

        badBlock = Block.empty(genesisBlock.getShaTwoFiftySix());
        badBlock.addTransaction(txId);
        badBlock.addTransaction(txBadIndex);
        badBlock.addReward(pair1.getPublic());
        mineBlock(badBlock);

        simulation.sendBlock(badBlock, 0);
        simulation.sendGetBlocksRequest(badBlock.getShaTwoFiftySix(), 1, 0);
        blocks = assertBlockMessage(simulation.getNextMessage());
        Assert.assertArrayEquals(empty, blocks);


        Transaction txBadAmount = new Transaction.Builder()
                .addInput(new TxIn(genesisBlock.getShaTwoFiftySix(), 0), pair1.getPrivate())
                .addOutput(new TxOut(Block.REWARD_AMOUNT + 1, pair1.getPublic()))
                .build();

        badBlock = Block.empty(genesisBlock.getShaTwoFiftySix());
        badBlock.addTransaction(txId);
        badBlock.addTransaction(txBadAmount);
        badBlock.addReward(pair1.getPublic());
        mineBlock(badBlock);

        simulation.sendBlock(badBlock, 0);
        simulation.sendGetBlocksRequest(badBlock.getShaTwoFiftySix(), 1, 0);
        blocks = assertBlockMessage(simulation.getNextMessage());
        Assert.assertArrayEquals(empty, blocks);


        Transaction txWrongOwner = new Transaction.Builder()
                .addInput(new TxIn(genesisBlock.getShaTwoFiftySix(), 0), pair0.getPrivate())
                .addOutput(new TxOut(Block.REWARD_AMOUNT, pair1.getPublic()))
                .build();

        badBlock = Block.empty(genesisBlock.getShaTwoFiftySix());
        badBlock.addTransaction(txId);
        badBlock.addTransaction(txWrongOwner);
        badBlock.addReward(pair1.getPublic());
        mineBlock(badBlock);

        simulation.sendBlock(badBlock, 0);
        simulation.sendGetBlocksRequest(badBlock.getShaTwoFiftySix(), 1, 0);
        blocks = assertBlockMessage(simulation.getNextMessage());
        Assert.assertArrayEquals(empty, blocks);


        Transaction txZeroOutput = new Transaction.Builder()
                .addInput(new TxIn(genesisBlock.getShaTwoFiftySix(), 0), pair1.getPrivate())
                .addOutput(new TxOut(Block.REWARD_AMOUNT, pair1.getPublic()))
                .addOutput(new TxOut(0, pair1.getPublic()))
                .build();

        badBlock = Block.empty(genesisBlock.getShaTwoFiftySix());
        badBlock.addTransaction(txId);
        badBlock.addTransaction(txZeroOutput);
        badBlock.addReward(pair1.getPublic());
        mineBlock(badBlock);

        simulation.sendBlock(badBlock, 0);
        simulation.sendGetBlocksRequest(badBlock.getShaTwoFiftySix(), 1, 0);
        blocks = assertBlockMessage(simulation.getNextMessage());
        Assert.assertArrayEquals(empty, blocks);


        Transaction txNegativeOutput = new Transaction.Builder()
                .addInput(new TxIn(genesisBlock.getShaTwoFiftySix(), 0), pair1.getPrivate())
                .addOutput(new TxOut(Block.REWARD_AMOUNT + 1, pair1.getPublic()))
                .addOutput(new TxOut(-1, pair1.getPublic()))
                .build();

        badBlock = Block.empty(genesisBlock.getShaTwoFiftySix());
        badBlock.addTransaction(txId);
        badBlock.addTransaction(txNegativeOutput);
        badBlock.addReward(pair1.getPublic());
        mineBlock(badBlock);

        simulation.sendBlock(badBlock, 0);
        simulation.sendGetBlocksRequest(badBlock.getShaTwoFiftySix(), 1, 0);
        blocks = assertBlockMessage(simulation.getNextMessage());
        Assert.assertArrayEquals(empty, blocks);
    }

    private Transaction assertTransactionMessage(Message msg) throws Exception {
        Assert.assertEquals(Message.TRANSACTION, msg.type);
        return Transaction.DESERIALIZER.deserialize(msg.payload);
    }

    private Block[] assertBlockMessage(Message msg) throws Exception {
        Assert.assertEquals(Message.BLOCK, msg.type);
        return Deserializer.deserializeList(msg.payload, Block.DESERIALIZER)
                .toArray(new Block[0]);
    }

    private Block assertSingleBlockMessage(Message msg) throws Exception {
        Block[] blocks = assertBlockMessage(msg);
        Assert.assertEquals(1, blocks.length);
        return blocks[0];
    }

    private static final class MinerSimulation {
        private final BlockingQueue<IncomingMessage> queue = new ArrayBlockingQueue<>(100);
        private final HashSet<Message> seenMessages = new HashSet<>();
        private final List<ConnectionThread> connectionThreads = new ArrayList<>();
        private final ServerSocket serverSocket;
        private final List<Integer> minerPortNums = new ArrayList<>();

        private MinerSimulation(int portNum) throws Exception {
            serverSocket = new ServerSocket(portNum);
        }

        private void addNode(int portNum, KeyPair keyPair, PublicKey privilegedKey) throws Exception {
            final List<Integer> portNumsToConnectTo = new ArrayList<>(minerPortNums);
            new Thread(() -> {
                Miner miner = new Miner(portNum, keyPair, privilegedKey);
                miner.connect("localhost", serverSocket.getLocalPort());
                for (int portNumToConnectTo : portNumsToConnectTo) {
                    miner.connect("localhost", portNumToConnectTo);
                }
                miner.startMiner();
            }).start();
            minerPortNums.add(portNum);
            ConnectionThread conn = new ConnectionThread(serverSocket.accept(), queue);
            conn.start();
            connectionThreads.add(conn);
            Thread.sleep(50); // give miner a chance to start
        }

        private void sendTransaction(Transaction transaction, int node) throws IOException {
            byte[] serialized = ByteUtil.asByteArray(transaction::serialize);
            sendMessage(new OutgoingMessage(Message.TRANSACTION, serialized), node);
        }

        private void sendBlock(Block block, int node) throws IOException {
            byte[] serialized = ByteUtil.asByteArray(block::serialize);
            sendMessage(new OutgoingMessage(Message.BLOCK, serialized), node);
        }

        private void sendGetBlocksRequest(ShaTwoFiftySix hash, int numRequested, int node) throws IOException {
            GetBlocksRequest request = new GetBlocksRequest(hash, numRequested);
            byte[] payload = ByteUtil.asByteArray(request::serialize);
            sendMessage(new OutgoingMessage(Message.GET_BLOCK, payload), node);
        }

        private void sendMessage(OutgoingMessage message, int node) throws IOException {
            connectionThreads.get(node).send(message);
        }

        private void sendBytes(byte[] bytes, int node) throws IOException {
            try (Socket socket = new Socket("localhost", minerPortNums.get(node))) {
                socket.getOutputStream().write(bytes);
            }
        }

        private Message getNextMessage() throws Exception {
            return getNextMessage(false);
        }

        private Message getNextMessage(boolean allowDuplicate) throws Exception {
            while (true) {
                Message msg = queue.poll(5, TimeUnit.SECONDS);
                Assert.assertNotNull(msg);
                if (allowDuplicate || !seenMessages.contains(msg)) {
                    seenMessages.add(msg);
                    return msg;
                }
            }
        }
    }

    private static void mineBlock(Block b) throws Exception {
        while(!b.checkHash()) {
            b.nonceAddOne();
        }
    }
}

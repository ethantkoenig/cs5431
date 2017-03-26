package network;


import block.Block;
import org.junit.Assert;
import org.junit.Test;
import testutils.RandomizedTest;
import testutils.TestUtils;
import transaction.Transaction;
import transaction.TxIn;
import transaction.TxOut;
import utils.ByteUtil;
import utils.Config;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
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

        MinerSimulation simulation = new MinerSimulation(10100, pair0, pair1);

        Block genesisBlock = assertSingleBlockMessage(simulation.getNextMessage());

        simulation.sendBytes(randomBytes(1024), 0); // should be ignored
        simulation.sendBytes(randomBytes(1024), 1); // should be ignored

        Thread.sleep(100); // TODO actually fix the race conditions

        simulation.sendMessage(new OutgoingMessage(
                Message.GET_BLOCK,
                Message.getBlockPayload(genesisBlock.getShaTwoFiftySix(), 1)
        ), 1);
        Block getBlockResponse = assertSingleBlockMessage(simulation.getNextMessage(true));
        Assert.assertEquals(genesisBlock, getBlockResponse);

        Transaction transaction1 = new Transaction.Builder()
                .addInput(
                        new TxIn(genesisBlock.getShaTwoFiftySix(), 0),
                        pair1.getPrivate()
                )
                .addOutput(new TxOut(Block.REWARD_AMOUNT, pair0.getPublic()))
                .build();
        simulation.sendTransaction(transaction1, 0);
        Transaction deserialized = assertTransactionMessage(simulation.getNextMessage());
        TestUtils.assertEqualsWithHashCode(errorMessage, transaction1, deserialized);

        Transaction transaction2 = new Transaction.Builder()
                .addInput(
                        new TxIn(transaction1.getShaTwoFiftySix(), 0),
                        pair0.getPrivate()
                )
                .addOutput(new TxOut(Block.REWARD_AMOUNT, pair1.getPublic()))
                .build();
        simulation.sendTransaction(transaction2, 1);
        deserialized = assertTransactionMessage(simulation.getNextMessage());
        TestUtils.assertEqualsWithHashCode(errorMessage, transaction2, deserialized);

        Block block = assertSingleBlockMessage(simulation.getNextMessage());
        boolean minedBy0 = block.reward.ownerPubKey.equals(pair0.getPublic());
        boolean minedBy1 = block.reward.ownerPubKey.equals(pair1.getPublic());
        Assert.assertTrue(minedBy0 || minedBy1);

        Thread.sleep(100); // TODO actually fix the race conditions

        simulation.sendMessage(new OutgoingMessage(
                Message.GET_BLOCK,
                Message.getBlockPayload(block.getShaTwoFiftySix(), 2)
        ), minedBy0 ? 0 : 1);
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

        simulation.assertMinersRunning();
    }


    private Transaction assertTransactionMessage(Message msg) throws Exception {
        Assert.assertEquals(Message.TRANSACTION, msg.type);
        return Transaction.deserialize(msg.payload);
    }

    private Block[] assertBlockMessage(Message msg) throws Exception {
        Assert.assertEquals(Message.BLOCK, msg.type);
        return TestUtils.assertPresent(
                Block.deserializeBlocks(msg.payload)
        );
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
        private final List<Thread> minerThreads = new ArrayList<>();
        private final int initPortNum;

        private MinerSimulation(int initPortNum, KeyPair... keyPairs) throws Exception {
            if (keyPairs.length == 0) {
                throw new IllegalArgumentException();
            }
            this.initPortNum = initPortNum;
            setUp(initPortNum, keyPairs);
        }

        private void setUp(int initPortNum, KeyPair[] keyPairs) throws Exception {
            ServerSocket serverSocket = new ServerSocket(initPortNum);

            KeyPair genesisPair = keyPairs[keyPairs.length - 1];
            for (int i = 0; i < keyPairs.length; i++) {
                KeyPair pair = keyPairs[i];
                final int minerPortNum = initPortNum + 1 + i;
                Thread minerThread = new Thread(() -> {
                    Miner miner = new Miner(minerPortNum, pair, genesisPair.getPublic());
                    for (int port = initPortNum; port < minerPortNum; port++) {
                        // connect to our ConnectionThread, and previous miners
                        miner.connect("localhost", port);
                    }
                    miner.startMiner();
                });
                minerThreads.add(minerThread);
                minerThread.start();
                ConnectionThread conn = new ConnectionThread(serverSocket.accept(), queue);
                conn.start();
                connectionThreads.add(conn);
                if (i < keyPairs.length - 1) {
                    Thread.sleep(50); // give miner a chance to start
                }
            }
        }

        private void assertMinersRunning() {
            for (Thread minerThread : minerThreads) {
                Assert.assertTrue(minerThread.isAlive());
            }
        }

        private void sendTransaction(Transaction transaction, int node) throws IOException {
            byte[] serialized = ByteUtil.asByteArray(transaction::serializeWithSignatures);
            sendMessage(new OutgoingMessage(Message.TRANSACTION, serialized), node);
        }

        private void sendMessage(OutgoingMessage message, int node) throws IOException {
            connectionThreads.get(node).send(message);
        }

        private void sendBytes(byte[] bytes, int node) throws IOException {
            Socket socket = new Socket("localhost", initPortNum + 1 + node);
            socket.getOutputStream().write(bytes);
            socket.close();
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
}

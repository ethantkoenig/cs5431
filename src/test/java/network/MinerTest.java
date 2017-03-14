package network;


import block.Block;
import block.UnspentTransactions;
import org.junit.Assert;
import org.junit.Test;
import testutils.RandomizedTest;
import testutils.TestUtils;
import transaction.Transaction;
import transaction.TxIn;
import transaction.TxOut;
import utils.ByteUtil;
import utils.Crypto;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.util.HashSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class MinerTest extends RandomizedTest {

    @Test
    public void testRun() throws Exception {
        KeyPair pair1 = Crypto.signatureKeyPair();
        KeyPair pair2 = Crypto.signatureKeyPair();

        MinerSimulation simulation = new MinerSimulation(pair1, pair2, 10100);

        Message genesisMessage = simulation.getNextMessage();
        Assert.assertEquals(genesisMessage.type, Message.BLOCK);
        Block genesisBlock = Block.deserialize(ByteBuffer.wrap(genesisMessage.payload));

        Transaction transaction1 = new Transaction.Builder()
                .addInput(
                        new TxIn(genesisBlock.getShaTwoFiftySix(), 0),
                        pair2.getPrivate()
                )
                .addOutput(new TxOut(Block.REWARD_AMOUNT, pair1.getPublic()))
                .build();

        simulation.sendTransaction(transaction1);

        Message transaction1Message = simulation.getNextMessage();
        Assert.assertEquals(transaction1Message.type, Message.TRANSACTION);
        Transaction deserialized = Transaction.deserialize(ByteBuffer.wrap(transaction1Message.payload));
        TestUtils.assertEqualsWithHashCode(errorMessage, transaction1, deserialized);

        Transaction transaction2 = new Transaction.Builder()
                .addInput(
                        new TxIn(transaction1.getShaTwoFiftySix(), 0),
                        pair1.getPrivate()
                )
                .addOutput(new TxOut(Block.REWARD_AMOUNT, pair2.getPublic()))
                .build();
        simulation.sendTransaction(transaction2);

        Message transaction2Message = simulation.getNextMessage();
        Assert.assertEquals(transaction2Message.type, Message.TRANSACTION);
        deserialized = Transaction.deserialize(ByteBuffer.wrap(transaction2Message.payload));
        TestUtils.assertEqualsWithHashCode(errorMessage, transaction2, deserialized);

        Message mineBlockMessage = simulation.getNextMessage();
        Assert.assertEquals(Message.BLOCK, mineBlockMessage.type);
        Block block = Block.deserialize(ByteBuffer.wrap(mineBlockMessage.payload));
        Assert.assertTrue(block.reward.ownerPubKey.equals(pair1.getPublic()) ||
                block.reward.ownerPubKey.equals(pair2.getPublic()));

        UnspentTransactions unspent = UnspentTransactions.empty();
        unspent.put(genesisBlock.getShaTwoFiftySix(), 0, genesisBlock.reward);
        Assert.assertTrue(block.verify(unspent).isPresent());
    }

    private static final class MinerSimulation {
        private final BlockingQueue<Message> queue = new ArrayBlockingQueue<>(100);
        private final HashSet<Message> seenMessages = new HashSet<>();
        private ConnectionThread connectionThread;

        private MinerSimulation(KeyPair pair1, KeyPair pair2, int portNum) throws Exception {
            setUp(pair1, pair2, portNum);
        }

        private void setUp(KeyPair pair1, KeyPair pair2, int portNum) throws Exception {
            new Thread(() -> {
                Miner miner1 = new Miner(portNum, pair1, pair2.getPublic());
                miner1.startMiner();
            }).start();

            Thread.sleep(200); // give connection threads a chance to start

            Socket socket = new Socket(InetAddress.getLocalHost(), portNum);
            connectionThread = new ConnectionThread(socket, queue);
            connectionThread.start();

            ServerSocket serverSocket = new ServerSocket(portNum + 1);
            new Thread(() -> {
                try {
                    new ConnectionThread(serverSocket.accept(), queue).start();
                } catch (IOException e) {
                    queue.add(null);
                }
            }).start();

            Thread.sleep(200); // give connection threads a chance to start

            new Thread(() -> {
                Miner miner2 = new Miner(portNum + 2, pair2, pair2.getPublic());
                miner2.connect("localhost", portNum);
                miner2.connect("localhost", portNum + 1);
                miner2.startMiner();
            }).start();
        }

        private void sendTransaction(Transaction transaction) throws IOException {
            byte[] serialized = ByteUtil.asByteArray(transaction::serializeWithSignatures);
            connectionThread.send(Message.TRANSACTION, serialized);
        }

        private Message getNextMessage() throws Exception {
            System.err.println("Starting getNextMessage");
            while (true) {
                Message msg = queue.poll(15, TimeUnit.SECONDS);
                Assert.assertNotNull(msg);
                System.err.println(String.format("Got message: (type=%d)", msg.type));
                if (!seenMessages.contains(msg)) {
                    System.err.println("Returning from getNextMessage");
                    seenMessages.add(msg);
                    return msg;
                }
            }
        }

    }
}
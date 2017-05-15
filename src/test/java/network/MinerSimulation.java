package network;

import block.Block;
import block.BlockChain;
import block.UnspentTransactions;
import crypto.Crypto;
import crypto.ECDSAKeyPair;
import crypto.ECDSAPrivateKey;
import crypto.ECDSAPublicKey;
import message.IncomingMessage;
import message.Message;
import message.OutgoingMessage;
import message.payloads.BlocksPayload;
import message.payloads.GetBlocksRequestPayload;
import message.payloads.PingPayload;
import message.payloads.PongPayload;
import org.junit.Assert;
import testutils.TestUtils;
import transaction.Transaction;
import transaction.TxIn;
import transaction.TxOut;
import utils.ByteUtil;
import utils.Deserializer;
import utils.ShaTwoFiftySix;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static testutils.RandomUtils.choiceList;

public final class MinerSimulation {
    private final Path tmpPath = Paths.get(System.getProperty("java.io.tmpdir"));

    private final List<TestMiner> miners = new ArrayList<>();
    private final Set<Transaction> transactions = new HashSet<>();
    private final ServerSocket serverSocket;
    private final ECDSAKeyPair privilegedKeyPair;
    private final BlockChain blockChain;
    private final Crypto crypto;

    public MinerSimulation(Crypto crypto) throws Exception {
        this(crypto, crypto.signatureKeyPair());
    }

    public MinerSimulation(Crypto crypto, ECDSAKeyPair privilegedKeyPair) throws IOException {
        this.crypto = crypto;
        serverSocket = new ServerSocket(0);
        this.privilegedKeyPair = privilegedKeyPair;
        Path blockChainPath = Files.createTempDirectory(tmpPath, "blockchain");
        this.blockChain = new BlockChain(blockChainPath);
    }

    public TestMiner addNode() throws Exception {
        final List<Integer> portNumsToConnectTo = miners.stream()
                .map(m -> m.portNumber).collect(Collectors.toList());
        return addNode(crypto.signatureKeyPair(), portNumsToConnectTo);
    }

    public TestMiner addNodeTo(int... minerIndices) throws Exception {
        final List<Integer> portNumsToConnectTo = Arrays.stream(minerIndices)
                .mapToObj(i -> miners.get(i).portNumber).collect(Collectors.toList());
        return addNode(crypto.signatureKeyPair(), portNumsToConnectTo);
    }

    public TestMiner addPrivileged() throws Exception {
        final List<Integer> portNumsToConnectTo = miners.stream()
                .map(m -> m.portNumber).collect(Collectors.toList());
        return addNode(privilegedKeyPair, portNumsToConnectTo);
    }

    public TestMiner addPrivilegedTo(int... minerIndices) throws Exception {
        final List<Integer> portNumsToConnectTo = Arrays.stream(minerIndices)
                .mapToObj(i -> miners.get(i).portNumber).collect(Collectors.toList());
        return addNode(privilegedKeyPair, portNumsToConnectTo);
    }

    private TestMiner addNode(ECDSAKeyPair keyPair, List<Integer> portNumsToConnectTo) throws Exception {
        ServerSocket socket = new ServerSocket(0);
        Path blockChainPath = Files.createTempDirectory(tmpPath, "blockchain");
        String name = "miner" + miners.size();
        Miner miner = new Miner(name, socket, keyPair,
                privilegedKeyPair.publicKey, blockChainPath);
        miner.connect("localhost", serverSocket.getLocalPort());
        for (int portNumToConnectTo : portNumsToConnectTo) {
            miner.connect("localhost", portNumToConnectTo);
        }
        new Thread(miner::startMiner).start();
        BlockingQueue<IncomingMessage> queue = new ArrayBlockingQueue<IncomingMessage>(25);
        Connection connection = Connection.accept(serverSocket.accept());
        ConnectionThread connThread = new ConnectionThread(connection, queue);
        connThread.start();
        TestMiner testMiner = new TestMiner(keyPair, queue, socket.getLocalPort(), connThread);
        miners.add(testMiner);
        return testMiner;
    }

    public Block addValidBlock(Random random) throws Exception {
        List<Transaction> transactions = validTransactions(random, Block.NUM_TRANSACTIONS_PER_BLOCK);
        for (Transaction transaction : transactions) {
            sendValidTransaction(choiceList(random, miners), transaction);
        }
        Block block = expectValidBlockFrom(choiceList(random, miners));
        flushQueues();
        return block;
    }

    public List<Transaction> validTransactions(Random random, int numTransactions) throws Exception {
        UnspentTransactions unspent = blockChain.getUnspentTransactionsAt(blockChain.getCurrentHead());
        List<Transaction> result = new ArrayList<>();
        for (int i = 0; i < numTransactions; i++) {
            ECDSAPublicKey recipientKey = choiceList(random, miners).keyPair.publicKey;

            List<Map.Entry<TxIn, TxOut>> entries = new ArrayList<>();
            unspent.forEach(entries::add);
            // disprefer rewards to account for race conditions
            List<Map.Entry<TxIn, TxOut>> nonRewardEntries = entries.stream()
                    .filter(e -> !blockChain.containsBlockWithHash(e.getKey().previousTxn))
                    .collect(Collectors.toList());
            Map.Entry<TxIn, TxOut> entry = !nonRewardEntries.isEmpty()
                    ? choiceList(random, nonRewardEntries)
                    : choiceList(random, entries);

            TxIn input = entry.getKey();
            TxOut output = entry.getValue();
            TxOut newOutput = new TxOut(output.value, recipientKey);
            Transaction transaction = new Transaction.Builder()
                    .addInput(input, privateOfPublic(output.ownerPubKey))
                    .addOutput(newOutput)
                    .build();
            unspent.remove(input.previousTxn, input.txIdx);
            unspent.put(transaction.getShaTwoFiftySix(), 0, newOutput);
            result.add(transaction);
        }
        return result;
    }

    private ECDSAPrivateKey privateOfPublic(ECDSAPublicKey publicKey) {
        return TestUtils.assertPresent(
                miners.stream().map(m -> m.keyPair)
                        .filter(p -> p.publicKey.equals(publicKey))
                        .map(p -> p.privateKey)
                        .findFirst()
        );
    }

    public Block expectGenesisBlock(TestMiner from) throws Exception {
        Block block = getSingleBlockMessage(from);
        Assert.assertTrue(block.isGenesisBlock());
        Assert.assertTrue(block.verifyGenesis(privilegedKeyPair.publicKey));
        blockChain.insertBlock(block);
        return block;
    }

    public Block expectValidBlockFrom(TestMiner repr) throws Exception {
        Block block = assertSingleBlockMessage(getNextMessage(repr));
        Block parent = TestUtils.assertPresent(blockChain.getBlockWithHash(block.previousBlockHash));
        UnspentTransactions unspent = blockChain.getUnspentTransactionsAt(parent);
        Assert.assertTrue(block.verifyNonGenesis(unspent).isPresent());
        Assert.assertEquals(1, miners.stream().map(m -> m.keyPair.publicKey)
                .filter(block.reward.ownerPubKey::equals)
                .count()
        );
        blockChain.insertBlock(block);
        return block;
    }

    public void sendValidTransaction(TestMiner repr, Transaction transaction) throws Exception {
        transactions.add(transaction);
        byte[] serialized = ByteUtil.asByteArray(transaction::serialize);
        sendMessage(repr, new OutgoingMessage(Message.TRANSACTION, serialized));
        IncomingMessage message = expectMessage(repr, msg -> msg.type == Message.TRANSACTION);
        Assert.assertNotNull(message);
        Assert.assertEquals(Message.TRANSACTION, message.type);
    }

    public void sendBlock(TestMiner repr, Block block) throws IOException {
        sendMessage(repr, new BlocksPayload(block).toMessage());
    }

    public void sendGetBlocksRequest(TestMiner repr, ShaTwoFiftySix hash, int numRequested) throws IOException {
        GetBlocksRequestPayload request = new GetBlocksRequestPayload(hash, numRequested);
        sendMessage(repr, request.toMessage());
    }

    public void sendMessage(TestMiner testMiner, OutgoingMessage message) throws IOException {
        testMiner.connectionThread.send(message);
    }

    public void sendBytes(TestMiner testMiner, byte[] bytes) throws IOException {
        try (Socket socket = new Socket("localhost", testMiner.portNumber)) {
            socket.getOutputStream().write(bytes);
        }
    }

    public Block[] getBlocksMessage(TestMiner testMiner) throws Exception {
        IncomingMessage message = getNextMessage(testMiner);
        return assertBlockMessage(message);
    }

    public Block getSingleBlockMessage(TestMiner testMiner) throws Exception {
        IncomingMessage message = getNextMessage(testMiner);
        return assertSingleBlockMessage(message);
    }

    public void flushQueues() throws Exception {
        Thread.sleep(100); // wait for ongoing activity to finish
        final int pingNumber = 12345;
        for (TestMiner miner : miners) {
            miner.connectionThread.send(new PingPayload(pingNumber).toMessage());
        }
        for (TestMiner miner : miners) {
            while (true) {
                IncomingMessage message = miner.queue.poll(500, TimeUnit.MILLISECONDS);
                Assert.assertNotNull(message);
                if (message.type == Message.BLOCKS) {
                    for (Block b : BlocksPayload.DESERIALIZER.deserialize(message.payload).blocks()) {
                        blockChain.insertBlock(b);
                    }
                } else if (message.type == Message.PONG) {
                    if (PongPayload.DESERIALIZER.deserialize(message.payload).pingNumber == pingNumber) {
                        break;
                    }
                }
            }
        }
    }

    public IncomingMessage getNextMessage(TestMiner testMiner) throws Exception {
        return getNextMessage(testMiner, 500);
    }

    public IncomingMessage getNextMessage(TestMiner testMiner, long timeout) throws Exception {
        while (true) {
            IncomingMessage msg = testMiner.queue.poll(timeout, TimeUnit.MILLISECONDS);
            Assert.assertNotNull(msg);
            if (msg.type == Message.TRANSACTION) {
                Transaction transaction = Transaction.DESERIALIZER.deserialize(msg.payload);
                if (transactions.contains(transaction)) {
                    continue;
                }
            }
            return msg;
        }
    }

    public IncomingMessage expectMessage(TestMiner testMiner,
                                         Predicate<IncomingMessage> predicate) throws Exception {
        while (true) {
            IncomingMessage msg = testMiner.queue.poll(500, TimeUnit.MILLISECONDS);
            Assert.assertNotNull(msg);
            if (predicate.test(msg)) {
                return msg;
            }
        }
    }

    public Optional<IncomingMessage> checkForMessage(TestMiner testMiner) throws Exception {
        return Optional.ofNullable(testMiner.queue.poll(1, TimeUnit.MILLISECONDS));
    }

    public void assertNoMessage() throws Exception {
        for (TestMiner miner : miners) {
            if (!miner.queue.isEmpty()) {
                IncomingMessage message = miner.queue.take();
                Assert.fail(String.format("Expected no message, found [type=%d]", message.type));
            }
        }
    }

    public static final class TestMiner {
        public final ECDSAKeyPair keyPair;
        private final BlockingQueue<IncomingMessage> queue;
        private final int portNumber;
        private final ConnectionThread connectionThread;

        private TestMiner(ECDSAKeyPair keyPair, BlockingQueue<IncomingMessage> queue, int portNumber, ConnectionThread connectionThread) {
            this.queue = queue;
            this.keyPair = keyPair;
            this.portNumber = portNumber;
            this.connectionThread = connectionThread;
        }
    }


    public static Transaction assertTransactionMessage(Message msg) throws Exception {
        Assert.assertEquals(Message.TRANSACTION, msg.type);
        return Transaction.DESERIALIZER.deserialize(msg.payload);
    }

    public static Block[] assertBlockMessage(Message msg) throws Exception {
        Assert.assertEquals(Message.BLOCKS, msg.type);
        return Deserializer.deserializeList(msg.payload, Block.DESERIALIZER)
                .toArray(new Block[0]);
    }

    public static Block[] assertBlocksMessage(IncomingMessage message, int numExpected) throws Exception {
        Block[] blocks = assertBlockMessage(message);
        Assert.assertEquals(numExpected, blocks.length);
        for (int i = 0; i < blocks.length - 1; i++) {
            Block child = blocks[i];
            Block parent = blocks[i + 1];
            Assert.assertEquals(child.previousBlockHash, parent.getShaTwoFiftySix());
        }
        return blocks;
    }

    public static Block assertSingleBlockMessage(Message msg) throws Exception {
        Block[] blocks = assertBlockMessage(msg);
        Assert.assertEquals(1, blocks.length);
        return blocks[0];
    }
}

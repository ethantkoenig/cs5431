package network;


import block.Block;
import message.IncomingMessage;
import message.Message;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import testutils.RandomizedTest;
import transaction.Transaction;
import transaction.TxIn;
import transaction.TxOut;
import utils.Config;
import utils.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import static network.MinerSimulation.assertBlocksMessage;
import static network.MinerSimulation.assertSingleBlockMessage;
import static testutils.TestUtils.assertEqualsWithHashCode;

public class MinerTest extends RandomizedTest {

    @Before
    public void setUpMinerTest() {
        Config.setHashGoal(1);
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$s %3$s%n%5$s%6$s%n");
        Log.parentLog().logger().setLevel(Level.INFO);
    }

    @Test
    public void testCatchUp() throws Exception {
        MinerSimulation simulation = new MinerSimulation(crypto);
        MinerSimulation.TestMiner miner0 = simulation.addNode();
        MinerSimulation.TestMiner miner1 = simulation.addPrivileged();

        Block genesisBlock = simulation.expectGenesisBlock(miner1);

        simulation.sendGetBlocksRequest(miner1, genesisBlock.getShaTwoFiftySix(), 1);
        Block getBlockResponse = simulation.getSingleBlockMessage(miner1);
        Assert.assertEquals(genesisBlock, getBlockResponse);

        simulation.flushQueues();

        final int numIters = random.nextInt(3 * Message.MAX_BLOCKS_TO_GET);
        for (int iter = 0; iter < numIters; iter++) {
            simulation.addValidBlock(random);
        }

        List<Transaction> transactions = simulation.validTransactions(random, 2);
        simulation.sendValidTransaction(miner0, transactions.get(0));
        MinerSimulation.TestMiner miner2 = simulation.addNode();
        simulation.sendValidTransaction(miner1, transactions.get(1));
        simulation.expectValidBlockFrom(miner0);
        simulation.flushQueues();

        transactions = simulation.validTransactions(random, 2);
        simulation.sendValidTransaction(miner0, transactions.get(0));
        simulation.sendValidTransaction(miner0, transactions.get(1));

        Block block = simulation.expectValidBlockFrom(miner1);
        simulation.flushQueues();

        IncomingMessage response = null;
        for (int i = 0; i < 15; i++) {
            simulation.sendGetBlocksRequest(miner2, block.getShaTwoFiftySix(), Message.MAX_BLOCKS_TO_GET);
            Optional<IncomingMessage> optMsg = simulation.checkForMessage(miner2);
            if (optMsg.isPresent()) {
                response = optMsg.get();
                break;
            }
            Thread.sleep(100); // wait for miner2 to get caught up
        }

        Assert.assertNotNull(response);
        final int expectedNumBlocks = Math.min(numIters + 3, Message.MAX_BLOCKS_TO_GET);
        Block[] blocks = assertBlocksMessage(response, expectedNumBlocks);
        Assert.assertEquals(block, blocks[0]);
        if (numIters + 3 < Message.MAX_BLOCKS_TO_GET) {
            Assert.assertEquals(genesisBlock, blocks[blocks.length - 1]);
        }
    }

    @Test
    public void testNotFullyConnected() throws Exception {
        MinerSimulation simulation = new MinerSimulation(crypto);
        MinerSimulation.TestMiner miner0 = simulation.addNode();
        MinerSimulation.TestMiner miner1 = simulation.addNode();
        MinerSimulation.TestMiner miner2 = simulation.addPrivilegedTo(1);

        Block genesisBlock = simulation.expectGenesisBlock(miner1);

        simulation.sendGetBlocksRequest(miner1, genesisBlock.getShaTwoFiftySix(), 1);
        Block getBlockResponse = simulation.getSingleBlockMessage(miner1);
        Assert.assertEquals(genesisBlock, getBlockResponse);

        simulation.flushQueues();

        final int numIters = random.nextInt(3 * Message.MAX_BLOCKS_TO_GET);
        for (int iter = 0; iter < numIters; iter++) {
            simulation.addValidBlock(random);
        }
        Block block = simulation.addValidBlock(random);

        for (MinerSimulation.TestMiner miner : Arrays.asList(miner0, miner1, miner2)) {
            simulation.sendGetBlocksRequest(miner, block.getShaTwoFiftySix(), Message.MAX_BLOCKS_TO_GET);
            IncomingMessage response = simulation.getNextMessage(miner);
            Block[] blocks = assertBlocksMessage(response, Message.MAX_BLOCKS_TO_GET);
            assertEqualsWithHashCode(block, blocks[0]);
        }
    }

    @Test
    public void testOldBlocks() throws Exception {
        MinerSimulation simulation = new MinerSimulation(crypto);
        MinerSimulation.TestMiner miner0 = simulation.addNode();
        MinerSimulation.TestMiner miner1 = simulation.addNode();
        MinerSimulation.TestMiner miner2 = simulation.addPrivileged();

        Block genesisBlock = simulation.expectGenesisBlock(miner0);
        assertEqualsWithHashCode(genesisBlock, simulation.expectGenesisBlock(miner1));
        assertEqualsWithHashCode(genesisBlock, simulation.expectGenesisBlock(miner2));

        simulation.flushQueues();

        Thread.sleep(50); // make sure other miners get the genesis block

        final int numIters = 2 + random.nextInt(3);
        for (int iter = 0; iter < numIters; iter++) {
            simulation.addValidBlock(random);
        }

        Transaction transaction1 = new Transaction.Builder()
                .addInput(new TxIn(genesisBlock.getShaTwoFiftySix(), 0), miner2.keyPair.privateKey)
                .addOutput(new TxOut(Block.REWARD_AMOUNT - 1, miner0.keyPair.publicKey))
                .addOutput(new TxOut(1, miner1.keyPair.publicKey))
                .build();
        Transaction transaction2 = new Transaction.Builder()
                .addInput(new TxIn(transaction1.getShaTwoFiftySix(), 0), miner0.keyPair.privateKey)
                .addOutput(new TxOut(Block.REWARD_AMOUNT - 1, miner1.keyPair.publicKey))
                .build();

        Block oldBlock = Block.empty(genesisBlock.getShaTwoFiftySix());
        oldBlock.addTransaction(transaction1);
        oldBlock.addTransaction(transaction2);
        oldBlock.addReward(miner1.keyPair.publicKey);
        oldBlock.findValidNonce();
        simulation.sendBlock(miner0, oldBlock);
        simulation.sendBlock(miner1, oldBlock);

        simulation.addValidBlock(random);
        simulation.addValidBlock(random);
    }

    @Test
    public void testLateGenesis() throws Exception {
        MinerSimulation simulation = new MinerSimulation(crypto);
        MinerSimulation.TestMiner miner0 = simulation.addNode();
        MinerSimulation.TestMiner miner1 = simulation.addPrivileged();

        Block genesisBlock = simulation.expectGenesisBlock(miner1);

        // Assert we get the rebroadcasted genesis block
        simulation.expectGenesisBlock(miner0);
        simulation.expectGenesisBlock(miner1);

        simulation.sendGetBlocksRequest(miner1, genesisBlock.getShaTwoFiftySix(), 1);
        Block getBlockResponse = simulation.getSingleBlockMessage(miner1);
        Assert.assertEquals(genesisBlock, getBlockResponse);

        Block badGenesis = Block.genesis();
        badGenesis.addReward(miner0.keyPair.publicKey);
        badGenesis.findValidNonce();
        simulation.sendBlock(miner0, badGenesis);
        simulation.sendBlock(miner1, badGenesis);
        simulation.sendGetBlocksRequest(miner0, badGenesis.getShaTwoFiftySix(), 1);
        simulation.sendGetBlocksRequest(miner1, badGenesis.getShaTwoFiftySix(), 1);
        simulation.expectMessage(miner0, msg -> msg.type == Message.BAD_REQUEST);
        simulation.expectMessage(miner1, msg -> msg.type == Message.BAD_REQUEST);
    }

    @Test
    public void testInvalidTransactionRejection() throws Exception {
        MinerSimulation simulation = new MinerSimulation(crypto);
        MinerSimulation.TestMiner miner0 = simulation.addNode();
        MinerSimulation.TestMiner miner1 = simulation.addPrivileged();

        Block genesisBlock = assertSingleBlockMessage(simulation.getNextMessage(miner1));

        // Assert we get the rebroadcasted genwsis blocks
        simulation.expectGenesisBlock(miner0);
        simulation.expectGenesisBlock(miner1);

        Transaction txId = new Transaction.Builder()
                .addInput(new TxIn(genesisBlock.getShaTwoFiftySix(), 0), miner1.keyPair.privateKey)
                .addOutput(new TxOut(Block.REWARD_AMOUNT, miner1.keyPair.publicKey))
                .build();

        // transactions that cannot validly come after txId
        List<Transaction> badTransactions = new ArrayList<>();

        Transaction txNonexistentBlock = new Transaction.Builder()
                .addInput(new TxIn(randomShaTwoFiftySix(), 0), miner0.keyPair.privateKey)
                .addOutput(new TxOut(50, miner0.keyPair.publicKey))
                .build();
        badTransactions.add(txNonexistentBlock);

        Transaction txBadIndex = new Transaction.Builder()
                .addInput(new TxIn(txId.getShaTwoFiftySix(), 1), miner1.keyPair.privateKey)
                .addOutput(new TxOut(Block.REWARD_AMOUNT, miner1.keyPair.publicKey))
                .build();
        badTransactions.add(txBadIndex);

        Transaction txBadAmount = new Transaction.Builder()
                .addInput(new TxIn(txId.getShaTwoFiftySix(), 0), miner1.keyPair.privateKey)
                .addOutput(new TxOut(Block.REWARD_AMOUNT + 1, miner1.keyPair.publicKey))
                .build();
        badTransactions.add(txBadAmount);

        Transaction txWrongOwner = new Transaction.Builder()
                .addInput(new TxIn(txId.getShaTwoFiftySix(), 0), miner0.keyPair.privateKey)
                .addOutput(new TxOut(Block.REWARD_AMOUNT, miner1.keyPair.publicKey))
                .build();
        badTransactions.add(txWrongOwner);

        Transaction txZeroOutput = new Transaction.Builder()
                .addInput(new TxIn(txId.getShaTwoFiftySix(), 0), miner1.keyPair.privateKey)
                .addOutput(new TxOut(Block.REWARD_AMOUNT, miner1.keyPair.publicKey))
                .addOutput(new TxOut(0, miner1.keyPair.publicKey))
                .build();
        badTransactions.add(txZeroOutput);

        Transaction txNegativeOutput = new Transaction.Builder()
                .addInput(new TxIn(txId.getShaTwoFiftySix(), 0), miner1.keyPair.privateKey)
                .addOutput(new TxOut(Block.REWARD_AMOUNT + 1, miner1.keyPair.publicKey))
                .addOutput(new TxOut(-1, miner1.keyPair.publicKey))
                .build();
        badTransactions.add(txNegativeOutput);

        for (Transaction badTransaction : badTransactions) {
            Block badBlock = Block.empty(genesisBlock.getShaTwoFiftySix());
            badBlock.addTransaction(txId);
            badBlock.addTransaction(badTransaction);
            badBlock.addReward(miner1.keyPair.publicKey);
            badBlock.findValidNonce();
            simulation.sendBlock(miner0, badBlock);
            simulation.sendGetBlocksRequest(miner0, badBlock.getShaTwoFiftySix(), 1);
            simulation.expectMessage(miner0, msg -> msg.type == Message.BAD_REQUEST);
        }
    }
}

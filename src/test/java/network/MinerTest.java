package network;


import block.Block;
import message.IncomingMessage;
import message.Message;
import org.junit.Assert;
import org.junit.Test;
import testutils.RandomizedTest;
import transaction.Transaction;
import transaction.TxIn;
import transaction.TxOut;
import utils.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static network.MinerSimulation.assertBlocksMessage;
import static network.MinerSimulation.assertSingleBlockMessage;

public class MinerTest extends RandomizedTest {

    @Test
    public void testCatchUp() throws Exception {
        Config.setHashGoal(1);

        MinerSimulation simulation = new MinerSimulation();
        MinerSimulation.TestMiner miner0 = simulation.addNode();
        MinerSimulation.TestMiner miner1 = simulation.addPrivileged();

        Block genesisBlock = simulation.expectGenesisBlock(miner1);

        simulation.sendGetBlocksRequest(miner1, genesisBlock.getShaTwoFiftySix(), 1);
        Block getBlockResponse = simulation.getSingleBlockMessage(miner1);
        Assert.assertEquals(genesisBlock, getBlockResponse);

        final int numIters = random.nextInt(3 * Message.MAX_BLOCKS_TO_GET);
        for (int iter = 0; iter < numIters; iter++) {
            simulation.addValidBlock(random);
        }

        List<Transaction> transactions = simulation.validTransactions(random, 2);
        simulation.sendValidTransaction(miner0, transactions.get(0));
        MinerSimulation.TestMiner miner2 = simulation.addNode();
        simulation.sendValidTransaction(miner1, transactions.get(1));
        simulation.expectValidBlockFromAny();
        simulation.flushQueues();

        transactions = simulation.validTransactions(random, 2);
        simulation.sendValidTransaction(miner0, transactions.get(0));
        simulation.sendValidTransaction(miner0, transactions.get(1));

        Block block = simulation.expectValidBlockFromAny();
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
    public void testOldBlocks() throws Exception {
        Config.setHashGoal(1);

        MinerSimulation simulation = new MinerSimulation();
        MinerSimulation.TestMiner miner0 = simulation.addNode();
        MinerSimulation.TestMiner miner1 = simulation.addNode();
        MinerSimulation.TestMiner miner2 = simulation.addPrivileged();

        Block genesisBlock = simulation.expectGenesisBlock(miner2);

        Thread.sleep(50); // make sure other miners get the genesis block

        final int numIters = 2 + random.nextInt(4);
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

        for (int iter = 0; iter < numIters; iter++) {
            simulation.addValidBlock(random);
        }
    }

    @Test
    public void testLateGenesis() throws Exception {
        MinerSimulation simulation = new MinerSimulation();
        MinerSimulation.TestMiner miner0 = simulation.addNode();
        MinerSimulation.TestMiner miner1 = simulation.addPrivileged();

        Block genesisBlock = simulation.expectGenesisBlock(miner1);

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

        // TODO assume that bad GET_BLOCKS request get no response
        simulation.assertNoMessage();
    }

    @Test
    public void testInvalidTransactionRejection() throws Exception {
        Config.setHashGoal(1);

        MinerSimulation simulation = new MinerSimulation();
        MinerSimulation.TestMiner miner0 = simulation.addNode();
        MinerSimulation.TestMiner miner1 = simulation.addPrivileged();

        Block genesisBlock = assertSingleBlockMessage(simulation.getNextMessage(miner1));

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
        }

        // should have never received response for any bad block
        // TODO assume that bad GET_BLOCKS request get no response
        simulation.assertNoMessage();
    }
}

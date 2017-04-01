package generators.model;

import block.MiningBlock;
import block.BlockChain;
import block.UnspentTransactions;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import transaction.Transaction;
import transaction.TxIn;
import transaction.TxOut;
import utils.ShaTwoFiftySix;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.ArrayList;

public class BlockChainGenerator extends Generator<BlockChain> {

    private static final int MIN_BLOCKS = 3;
    private static final int MAX_BLOCKS = 20;

    public BlockChainGenerator() {
        super(BlockChain.class);
    }

    @Override
    public BlockChain generate(SourceOfRandomness random, GenerationStatus status) {
        MiningBlock genesis = MiningBlock.genesis();
        KeyPair privilegedKey = gen().type(KeyPair.class).generate(random, status);

        genesis.addReward(privilegedKey.getPublic());

        try {
            while (!genesis.checkHash()) {
                genesis.nonceAddOne();
            }
        } catch (Exception e) {
            // We should not reach here
            e.printStackTrace();
            assert false;
            return null;
        }

        Path blockChainPath;
        try {
            blockChainPath = Files.createTempDirectory("test");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        BlockChain blockchain = new BlockChain(blockChainPath, genesis);

        MiningBlock second = MiningBlock.empty(genesis.getShaTwoFiftySix());
        ShaTwoFiftySix prevHash = genesis.getShaTwoFiftySix();
        try {
            for (int i = 0; i < MiningBlock.NUM_TRANSACTIONS_PER_BLOCK - 1; ++i) {
                Transaction dummy = new Transaction.Builder()
                        .addInput(
                                new TxIn(prevHash, 0),
                                privilegedKey.getPrivate())
                        .addOutput(
                                new TxOut(MiningBlock.REWARD_AMOUNT, privilegedKey.getPublic()))
                        .build();
                prevHash = dummy.getShaTwoFiftySix();
                second.addTransaction(dummy);
            }
        } catch (IOException | GeneralSecurityException e) {
            // We should not reach this case
            e.printStackTrace();
            assert false;
            return null;
        }

        Transaction.Builder txBuilder = new Transaction.Builder();
        txBuilder.addInput(new TxIn(prevHash, 0), privilegedKey.getPrivate());

        long valueLeft = MiningBlock.REWARD_AMOUNT;
        int numOut = 20;
        for (int i = 0; i < numOut; ++i) {
            long outVal;
            if (i == numOut - 1) {
                outVal = valueLeft;
            } else {
                outVal = MiningBlock.REWARD_AMOUNT / numOut;
            }
            valueLeft -= outVal;

            TxOut out = new TxOut(outVal, privilegedKey.getPublic());
            txBuilder.addOutput(out);
        }

        Transaction distributer;
        try {
            distributer = txBuilder.build();
        } catch (IOException | GeneralSecurityException e) {
            // We should not reach this case
            e.printStackTrace();
            assert false;
            return null;
        }

        second.addTransaction(distributer);
        second.addReward(privilegedKey.getPublic());

        try {
            while (!second.checkHash()) {
                second.nonceAddOne();
            }
        } catch (Exception e) {
            // We should not reach here
            e.printStackTrace();
            assert false;
            return null;
        }

        blockchain.insertBlock(second);

        ArrayList<MiningBlock> blocksToGrowFrom = new ArrayList<>();
        blocksToGrowFrom.add(second);

        int numBlocksToPut = random.nextInt(MIN_BLOCKS, MAX_BLOCKS);

        for (int i = 0; i < numBlocksToPut; ++i) {
            MiningBlock parent = blocksToGrowFrom.get(
                    random.nextInt(0, blocksToGrowFrom.size() - 1));
            UnspentTransactions unspentTxs = blockchain.getUnspentTransactionsAt(parent);
            MiningBlock child = new BlockGenerator().generate(
                    parent.getShaTwoFiftySix(),
                    unspentTxs,
                    random,
                    status);
            blockchain.insertBlock(child);
            blocksToGrowFrom.add(child);
        }

        return blockchain;
    }
}

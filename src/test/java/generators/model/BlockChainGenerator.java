package generators.model;

import block.Block;
import block.BlockChain;
import block.UnspentTransactions;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import crypto.ECDSAKeyPair;
import org.junit.Assert;
import transaction.Transaction;
import transaction.TxIn;
import transaction.TxOut;
import utils.ShaTwoFiftySix;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class BlockChainGenerator extends Generator<BlockChain> {

    private static final int MIN_BLOCKS = 3;
    private static final int MAX_BLOCKS = 10;

    public BlockChainGenerator() {
        super(BlockChain.class);
    }

    @Override
    public BlockChain generate(SourceOfRandomness random, GenerationStatus status) {
        ECDSAKeyPair privilegedKey = gen().type(ECDSAKeyPair.class).generate(random, status);
        Block genesis = Block.genesis(privilegedKey.publicKey);

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

        ShaTwoFiftySix prevHash = genesis.getShaTwoFiftySix();
        List<Transaction> transactions = new ArrayList<>();
        try {
            for (int i = 0; i < Block.NUM_TRANSACTIONS_PER_BLOCK - 1; ++i) {
                Transaction dummy = new Transaction.Builder()
                        .addInput(
                                new TxIn(prevHash, 0),
                                privilegedKey.privateKey)
                        .addOutput(
                                new TxOut(Block.REWARD_AMOUNT, privilegedKey.publicKey))
                        .build();
                prevHash = dummy.getShaTwoFiftySix();
                transactions.add(dummy);
            }
        } catch (IOException e) {
            // We should not reach this case
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        Transaction.Builder txBuilder = new Transaction.Builder();
        txBuilder.addInput(new TxIn(prevHash, 0), privilegedKey.privateKey);

        long valueLeft = Block.REWARD_AMOUNT;
        int numOut = 20;
        for (int i = 0; i < numOut; ++i) {
            long outVal;
            if (i == numOut - 1) {
                outVal = valueLeft;
            } else {
                outVal = Block.REWARD_AMOUNT / numOut;
            }
            valueLeft -= outVal;

            TxOut out = new TxOut(outVal, privilegedKey.publicKey);
            txBuilder.addOutput(out);
        }

        Transaction distributer;
        try {
            distributer = txBuilder.build();
        } catch (IOException e) {
            // We should not reach this case
            e.printStackTrace();
            assert false;
            return null;
        }

        transactions.add(distributer);
        Block second = Block.block(genesis.getShaTwoFiftySix(), transactions, privilegedKey.publicKey);

        try {
            second.findValidNonce();
        } catch (IOException e) {
            // We should not reach here
            e.printStackTrace();
            assert false;
            return null;
        }

        blockchain.insertBlock(second);

        ArrayList<Block> blocksToGrowFrom = new ArrayList<>();
        blocksToGrowFrom.add(second);

        int numBlocksToPut = random.nextInt(MIN_BLOCKS, MAX_BLOCKS);

        for (int i = 0; i < numBlocksToPut; ++i) {
            Block parent = blocksToGrowFrom.get(
                    random.nextInt(0, blocksToGrowFrom.size() - 1));
            UnspentTransactions unspentTxs = blockchain.getUnspentTransactionsAt(parent);
            Block child = new BlockGenerator().generate(
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

package generators.model;

import block.Block;
import block.UnspentTransactions;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import crypto.ECDSAKeyPair;
import utils.ShaTwoFiftySix;

public class BlockGenerator extends Generator<Block> {

    public BlockGenerator() {
        super(Block.class);
    }

    @Override
    public Block generate(SourceOfRandomness random, GenerationStatus status) {
        UnspentTransactions unspentTxs =
                new UnspentTransactionsGenerator().generate(random, status);
        ShaTwoFiftySix randomHash = gen().type(ShaTwoFiftySix.class).generate(random, status);
        return generate(randomHash, unspentTxs, random, status);
    }

    public Block generate(
            ShaTwoFiftySix previousBlock,
            UnspentTransactions unspentTxs,
            SourceOfRandomness random,
            GenerationStatus status) {
        Block block = Block.empty(previousBlock);
        TransactionGenerator txGen = new TransactionGenerator(unspentTxs);
        for (int i = 0; i < Block.NUM_TRANSACTIONS_PER_BLOCK; ++i) {
            block.addTransaction(txGen.generate(random, status));
        }

        ECDSAKeyPair keys = new SigningKeyPairGenerator().generate(random, status);

        block.addReward(keys.publicKey);

        try {
            while (!block.checkHash()) {
                block.nonceAddOne();
            }
        } catch (Exception e) {
            // We should not reach this case if the block was constructed correctly
            e.printStackTrace();
            assert false;
            return null;
        }

        unspentTxs.put(block.getShaTwoFiftySix(), 0, block.reward);
        return block;
    }
}

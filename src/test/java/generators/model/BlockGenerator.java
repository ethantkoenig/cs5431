package generators.model;

import block.MiningBlock;
import block.UnspentTransactions;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import utils.ShaTwoFiftySix;

import java.security.KeyPair;

public class BlockGenerator extends Generator<MiningBlock> {

    public BlockGenerator() {
        super(MiningBlock.class);
    }

    @Override
    public MiningBlock generate(SourceOfRandomness random, GenerationStatus status) {
        UnspentTransactions unspentTxs =
                new UnspentTransactionsGenerator().generate(random, status);
        ShaTwoFiftySix randomHash = gen().type(ShaTwoFiftySix.class).generate(random, status);
        return generate(randomHash, unspentTxs, random, status);
    }

    public MiningBlock generate(
            ShaTwoFiftySix previousBlock,
            UnspentTransactions unspentTxs,
            SourceOfRandomness random,
            GenerationStatus status) {
        MiningBlock block = MiningBlock.empty(previousBlock);
        TransactionGenerator txGen = new TransactionGenerator(unspentTxs);
        for (int i = 0; i < MiningBlock.NUM_TRANSACTIONS_PER_BLOCK; ++i) {
            block.addTransaction(txGen.generate(random, status));
        }

        KeyPair keys = new SigningKeyPairGenerator().generate(random, status);

        block.addReward(keys.getPublic());

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

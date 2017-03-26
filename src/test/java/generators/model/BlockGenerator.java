package generators.model;

import block.Block;
import block.UnspentTransactions;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import utils.Pair;
import utils.ShaTwoFiftySix;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;

public class BlockGenerator extends Generator<Block> {

    public BlockGenerator() {
        super(Block.class);
    }

    @Override
    public Block generate(SourceOfRandomness random, GenerationStatus status) {
        Pair<UnspentTransactions, Map<PublicKey, PrivateKey>> pair =
                new UnspentTransactionsGenerator().generateWithKeys(random, status);
        ShaTwoFiftySix randomHash = gen().type(ShaTwoFiftySix.class).generate(random, status);
        return generate(randomHash, pair.getLeft(), pair.getRight(), random, status);
    }

    public Block generate(
            ShaTwoFiftySix previousBlock,
            UnspentTransactions unspentTxs,
            Map<PublicKey, PrivateKey> keyMapping,
            SourceOfRandomness random,
            GenerationStatus status)
    {
        Block block = Block.empty(previousBlock);
        TransactionGenerator txGen = new TransactionGenerator(unspentTxs, keyMapping);
        for (int i = 0; i < Block.NUM_TRANSACTIONS_PER_BLOCK; ++i) {
            block.addTransaction(txGen.generate(random, status));
        }

        KeyPair keys = gen().type(KeyPair.class).generate(random, status);
        keyMapping.put(keys.getPublic(), keys.getPrivate());

        block.addReward(keys.getPublic());

        try {
            while (!block.checkHash()) {
                block.nonceAddOne();
            }
        } catch (Exception e) {
            // We should not reach this case if the block was constructed correctly
            assert false;
            e.printStackTrace();
            return null;
        }

        unspentTxs.put(block.getShaTwoFiftySix(), 0, block.reward);
        return block;
    }
}

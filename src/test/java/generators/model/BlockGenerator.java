package generators.model;

import block.Block;
import block.UnspentTransactions;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import crypto.ECDSAKeyPair;
import transaction.Transaction;
import utils.ShaTwoFiftySix;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        ECDSAKeyPair keys = new SigningKeyPairGenerator().generate(random, status);
        TransactionGenerator txGen = new TransactionGenerator(unspentTxs);

        List<Transaction> transactions = IntStream.range(0, Block.NUM_TRANSACTIONS_PER_BLOCK)
                .mapToObj(i -> txGen.generate(random, status))
                .collect(Collectors.toList());

        Block block = Block.block(previousBlock, transactions, keys.publicKey);

        try {
            block.findValidNonce();
        } catch (IOException e) {
            // We should not reach this case if the block was constructed correctly
            e.printStackTrace();
            assert false;
            return null;
        }

        unspentTxs.put(block.getShaTwoFiftySix(), 0, block.reward);
        return block;
    }
}

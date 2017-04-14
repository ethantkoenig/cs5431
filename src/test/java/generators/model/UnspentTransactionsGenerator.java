package generators.model;

import block.UnspentTransactions;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.generator.Size;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import testutils.Generators;
import transaction.TxOut;
import utils.ShaTwoFiftySix;

import java.util.ArrayList;

public class UnspentTransactionsGenerator extends Generator<UnspentTransactions> {

    private Size size;

    private static final int DEFAULT_MIN_SIZE = 0;
    private static final int DEFAULT_MAX_SIZE = 20;

    public UnspentTransactionsGenerator() {
        super(UnspentTransactions.class);
    }

    public void configure(Size size) {
        this.size = size;
    }

    @Override
    public UnspentTransactions generate(SourceOfRandomness random, GenerationStatus status) {
        UnspentTransactions unspentTxs = UnspentTransactions.empty();

        int minSize = size == null ? DEFAULT_MIN_SIZE : size.min();
        int maxSize = size == null ? DEFAULT_MAX_SIZE : size.max();
        int numTxs = random.nextInt(minSize, maxSize);

        ArrayList<ShaTwoFiftySix> hashes = Generators.generateList(
                numTxs, gen().type(ShaTwoFiftySix.class), random, status);

        ArrayList<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < numTxs; ++i) {
            offsets.add(random.nextInt(0, 15));
        }

        Generator<TxOut> outGen = gen().type(TxOut.class);
        ArrayList<TxOut> outputs = new ArrayList<>();
        for (int i = 0; i < numTxs; ++i) {
            TxOut out = outGen.generate(random, status);
            outputs.add(out);
        }

        for (int i = 0; i < numTxs; ++i) {
            unspentTxs.put(hashes.get(i), offsets.get(i), outputs.get(i));
        }

        return unspentTxs;
    }
}

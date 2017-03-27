package generators.model;

import block.UnspentTransactions;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import transaction.TxIn;
import transaction.TxOut;
import utils.Pair;
import utils.ShaTwoFiftySix;

import java.util.Map;

public class TxInGenerator extends Generator<TxIn> {

    private static int MIN_IDX = 0;
    private static int MAX_IDX = 100;

    public TxInGenerator() {
        super(TxIn.class);
    }

    @Override
    public TxIn generate(SourceOfRandomness random, GenerationStatus status) {
        return new TxIn(gen().type(ShaTwoFiftySix.class).generate(random, status),
                random.nextInt(MIN_IDX, MAX_IDX));
    }

    public Pair<TxOut, TxIn> generateWithRespectTo(UnspentTransactions unspentTxs, SourceOfRandomness random, GenerationStatus status) {
        int index = random.nextInt(0, unspentTxs.size() - 1);

        int i = 0;
        for (Map.Entry<Pair<ShaTwoFiftySix, Integer>, TxOut> e: unspentTxs) {
            if (i == index) {
                unspentTxs.remove(e.getKey().getLeft(), e.getKey().getRight());
                return new Pair<>(
                        e.getValue(),
                        new TxIn(e.getKey().getLeft(), e.getKey().getRight()));
            }
            i += 1;
        }

        assert false;
        return null;
    }

}

package generators.model;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import transaction.TxIn;
import utils.ShaTwoFiftySix;

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
}

package generators.model;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import transaction.TxOut;
import utils.Pair;

import java.security.KeyPair;

public class TxOutGenerator extends Generator<TxOut> {

    private static int MIN_VALUE = 0;
    private static int MAX_VALUE = 100;

    public TxOutGenerator() {
        super(TxOut.class);
    }

    @Override
    public TxOut generate(SourceOfRandomness random, GenerationStatus status) {
        return new TxOut(random.nextLong(MIN_VALUE, MAX_VALUE),
                gen().type(KeyPair.class).generate(random, status).getPublic());
    }
}

package testutils;

import com.pholser.junit.quickcheck.generator.ComponentizedGenerator;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import utils.Pair;

public class PairGenerator extends ComponentizedGenerator<Pair> {

    public PairGenerator() {
        super(Pair.class);
    }

    @Override
    public Pair<?, ?> generate(SourceOfRandomness random, GenerationStatus status) {
        return new Pair<>(
                componentGenerators().get(0).generate(random, status),
                componentGenerators().get(1).generate(random, status));
    }
}

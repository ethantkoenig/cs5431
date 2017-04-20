package generators.model;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import utils.ShaTwoFiftySix;

public class ShaTwoFiftySixGenerator extends Generator<ShaTwoFiftySix> {

    public ShaTwoFiftySixGenerator() {
        super(ShaTwoFiftySix.class);
    }

    @Override
    public ShaTwoFiftySix generate(SourceOfRandomness random, GenerationStatus status) {
        return ShaTwoFiftySix.create(random.nextBytes(ShaTwoFiftySix.HASH_SIZE_IN_BYTES)).get();
    }
}

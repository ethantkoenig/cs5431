package testutils;

import java.util.Random;

/**
 * A utility class for reproducible, randomized testutils.
 */
public class SeededRandom {
    private static final Random classRandom = new Random();

    private final long seed;
    private final Random random;

    private SeededRandom(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
    }

    /**
     * @return A testutils.SeededRandom with a random seed
     */
    public static SeededRandom randomSeed() {
        return new SeededRandom(classRandom.nextLong());
    }

    /**
     * @return A testutils.SeededRandom with the given seed
     */
    public static SeededRandom fixedSeed(long seed) {
        return new SeededRandom(seed);
    }

    public Random random() {
        return random;
    }

    /**
     * @return An error message containing the seed
     */
    public String errorMessage() {
        return String.format("Failed with seed %d", seed);
    }
}

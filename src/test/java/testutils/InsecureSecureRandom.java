package testutils;


import java.security.SecureRandom;
import java.util.Random;

/**
 * An insecure implementation of the {@code SecureRandom} class. For testing
 * only.
 */
public class InsecureSecureRandom extends SecureRandom {
    private final Random random;
    public InsecureSecureRandom(Random random ) {
        super();
        this.random = random;
    }

    @Override
    public void nextBytes(byte[] bytes) {
        random.nextBytes(bytes);
    }
}

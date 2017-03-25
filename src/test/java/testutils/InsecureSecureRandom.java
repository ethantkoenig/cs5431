package testutils;


import java.security.SecureRandom;
import java.security.SecureRandomSpi;
import java.util.Random;

/**
 * An insecure subclass of {@code SecureRandom} class. For testing only.
 */
public class InsecureSecureRandom extends SecureRandom {
    private static final long serialVersionUID = 42L;
    private final Random random;

    public InsecureSecureRandom(Random random) {
        super(new InsecureSecureRandomSpi(random), null);
        this.random = random;
    }

    @Override
    public void nextBytes(byte[] bytes) {
        random.nextBytes(bytes);
    }

    private static class InsecureSecureRandomSpi extends SecureRandomSpi {
        private static final long serialVersionUID = 42L;
        private final Random random;

        private InsecureSecureRandomSpi(Random random) {
            this.random = random;
        }

        @Override
        protected void engineSetSeed(byte[] bytes) {
        }

        @Override
        protected void engineNextBytes(byte[] bytes) {
            random.nextBytes(bytes);
        }

        @Override
        protected byte[] engineGenerateSeed(int numBytes) {
            byte[] result = new byte[numBytes];
            random.nextBytes(result);
            return result;
        }
    }
}

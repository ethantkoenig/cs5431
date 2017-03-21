package generators.model;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import utils.Crypto;

import java.security.GeneralSecurityException;
import java.security.KeyPair;

public class SigningKeyPairGenerator extends Generator<KeyPair> {

    private static int MAX_TRIES = 100;

    public SigningKeyPairGenerator() {
        super(KeyPair.class);
    }

    @Override
    public KeyPair generate(SourceOfRandomness random, GenerationStatus status) {
        for (int i = 0; i < MAX_TRIES; ++i) {
            try {
                // TODO: incorporate SourceOfRandomness, handle crypto.init()
                return Crypto.signatureKeyPair();
            } catch (GeneralSecurityException e) {
                // try again
            }
        }
        return null;
    }
}

package generators.model;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import org.junit.Before;
import org.junit.BeforeClass;
import testutils.InsecureSecureRandom;
import testutils.SeededRandom;
import utils.Crypto;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Random;

public class SigningKeyPairGenerator extends Generator<KeyPair> {

    private static int MAX_TRIES = 100;

    public SigningKeyPairGenerator() {
        super(KeyPair.class);
    }

    @BeforeClass
    public static void setupClass() {
        Crypto.init();
    }

    @Override
    public KeyPair generate(SourceOfRandomness random, GenerationStatus status) {
        SeededRandom seededRandom = SeededRandom.fixedSeed(random.nextLong());
        Random rand = seededRandom.random();

        KeyPairGenerator generator = null;
        try {
            generator = KeyPairGenerator.getInstance("ECDSA", "BC");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }

        ECGenParameterSpec ecSpec = new ECGenParameterSpec("P-256");
        SecureRandom secureRandom = new InsecureSecureRandom(rand);
        try {
            generator.initialize(ecSpec, secureRandom);
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }

        return generator.generateKeyPair();
    }
}

package generators.model;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import org.junit.BeforeClass;
import testutils.InsecureSecureRandom;
import testutils.SeededRandom;
import utils.Crypto;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SigningKeyPairGenerator extends Generator<KeyPair> {

    private static final Map<PublicKey, PrivateKey> keyMapping = new HashMap<>();

    public static Map<PublicKey, PrivateKey> getKeyMapping() {
        return keyMapping;
    }

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
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            e.printStackTrace();
            assert false;
            return null;
        }

        ECGenParameterSpec ecSpec = new ECGenParameterSpec("P-256");
        SecureRandom secureRandom = new InsecureSecureRandom(rand);
        try {
            generator.initialize(ecSpec, secureRandom);
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }

        KeyPair keys = generator.generateKeyPair();
        keyMapping.put(keys.getPublic(), keys.getPrivate());

        return keys;
    }
}
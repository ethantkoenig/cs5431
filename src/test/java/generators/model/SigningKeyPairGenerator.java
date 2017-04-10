package generators.model;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import crypto.Crypto;
import crypto.ECDSAKeyPair;
import crypto.ECDSAPrivateKey;
import crypto.ECDSAPublicKey;
import org.junit.BeforeClass;
import testutils.InsecureSecureRandom;
import utils.*;

import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

public class SigningKeyPairGenerator extends Generator<ECDSAKeyPair> {

    private static final Map<ECDSAPublicKey, ECDSAPrivateKey> keyMapping = new HashMap<>();

    public static Map<ECDSAPublicKey, ECDSAPrivateKey> getKeyMapping() {
        return keyMapping;
    }

    public SigningKeyPairGenerator() {
        super(ECDSAKeyPair.class);
    }

    @BeforeClass
    public static void setupClass() {
        Crypto.init();
    }

    @Override
    public ECDSAKeyPair generate(SourceOfRandomness random, GenerationStatus status) {
        Config.setSecureRandom(new InsecureSecureRandom(random.toJDKRandom()));
        try {
            ECDSAKeyPair keys = Crypto.signatureKeyPair();
            keyMapping.put(keys.publicKey, keys.privateKey);
            return keys;
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return null;
        }
    }
}

package generators.model;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import crypto.Crypto;
import crypto.ECDSAKeyPair;
import crypto.ECDSAPrivateKey;
import crypto.ECDSAPublicKey;
import testutils.InsecureSecureRandom;

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

    @Override
    public ECDSAKeyPair generate(SourceOfRandomness random, GenerationStatus status) {
        try {
            Crypto crypto = new Crypto(new InsecureSecureRandom(random.toJDKRandom()));
            ECDSAKeyPair keys = crypto.signatureKeyPair();
            keyMapping.put(keys.publicKey, keys.privateKey);
            return keys;
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return null;
        }
    }
}

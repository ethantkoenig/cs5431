package generators.server.models;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import org.junit.BeforeClass;
import server.models.Key;
import utils.Crypto;

import java.security.KeyPair;

/**
 * Created by Eric on 3/28/2017.
 */
public class KeyGenerator extends Generator<Key> {

    @BeforeClass
    public void initCrypto() {
        Crypto.init();
    }

    public KeyGenerator() {
        super(Key.class);
    }

    @Override
    public Key generate(SourceOfRandomness random, GenerationStatus status) {
        KeyPair keys = gen().type(KeyPair.class).generate(random, status);
        // TODO: encrypt the private key here instead of passing it in in plaintext
        return new Key(keys.getPublic().getEncoded(), keys.getPrivate().getEncoded());
    }
}

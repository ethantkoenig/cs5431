package generators.server.models;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import crypto.Crypto;
import crypto.ECDSAKeyPair;
import org.junit.BeforeClass;
import server.models.Key;
import utils.ByteUtil;

public class KeyGenerator extends Generator<Key> {

    @BeforeClass
    public static void initCrypto() {
        Crypto.init();
    }

    public KeyGenerator() {
        super(Key.class);
    }

    @Override
    public Key generate(SourceOfRandomness random, GenerationStatus status) {
        ECDSAKeyPair pair = gen().type(ECDSAKeyPair.class).generate(random, status);
        return new Key(
                random.nextInt(1024),
                random.nextInt(1024),
                ByteUtil.forceByteArray(pair.publicKey::serialize),
                ByteUtil.bytesToHexString(
                        ByteUtil.forceByteArray(pair.privateKey::serialize)
                )
        );
    }
}

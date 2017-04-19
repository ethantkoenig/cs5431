package generators.model;


import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import crypto.Crypto;
import crypto.ECDSAKeyPair;
import crypto.ECDSASignature;

public class ECDSASignatureGenerator extends Generator<ECDSASignature> {

    public ECDSASignatureGenerator() {
        super(ECDSASignature.class);
    }

    @Override
    public ECDSASignature generate(SourceOfRandomness sourceOfRandomness, GenerationStatus generationStatus) {
        ECDSAKeyPair pair = new SigningKeyPairGenerator().generate(sourceOfRandomness, generationStatus);
        byte[] content = sourceOfRandomness.nextBytes(1024);
        return Crypto.sign(content, pair.privateKey);
    }
}

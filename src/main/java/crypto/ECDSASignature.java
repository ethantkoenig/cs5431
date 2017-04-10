package crypto;

import utils.CanBeSerialized;
import utils.DeserializationException;
import utils.Deserializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;

public final class ECDSASignature implements CanBeSerialized {
    public static final Deserializer<ECDSASignature> DESERIALIZER = new ECDSASignatureDeserializer();

    private static final int MAX_COORD_LENGTH_IN_BYTES = 34;

    public final BigInteger r;
    public final BigInteger s;

    public ECDSASignature(BigInteger r, BigInteger s) {
        this.r = r;
        this.s = s;
    }

    @Override
    public void serialize(DataOutputStream outputStream) throws IOException {
        CanBeSerialized.serializeBigInteger(outputStream, r);
        CanBeSerialized.serializeBigInteger(outputStream, s);
    }

    private static class ECDSASignatureDeserializer implements Deserializer<ECDSASignature> {
        @Override
        public ECDSASignature deserialize(DataInputStream inputStream) throws DeserializationException, IOException {
            BigInteger r = Deserializer.deserializeBigInteger(inputStream, MAX_COORD_LENGTH_IN_BYTES);
            BigInteger s = Deserializer.deserializeBigInteger(inputStream, MAX_COORD_LENGTH_IN_BYTES);
            return new ECDSASignature(r, s);
        }
    }
}

package utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;

public class ECDSAPrivateKey implements CanBeSerialized {
    public static final Deserializer<ECDSAPrivateKey> DESERIALIZER = new ECDSAPrivateKeyDeserializer();
    private static final int MAX_COORD_LENGTH_IN_BYTES = 66;
    public final BigInteger d;

    public ECDSAPrivateKey(BigInteger d) {
        this.d = d;
    }

    @Override
    public void serialize(DataOutputStream outputStream) throws IOException {
        CanBeSerialized.serializeBigInteger(outputStream, d);
    }

    private static class ECDSAPrivateKeyDeserializer implements Deserializer<ECDSAPrivateKey> {
        @Override
        public ECDSAPrivateKey deserialize(DataInputStream inputStream) throws DeserializationException, IOException {
            return new ECDSAPrivateKey(Deserializer.deserializeBigInteger(inputStream, MAX_COORD_LENGTH_IN_BYTES));
        }
    }
}

package crypto;

import utils.CanBeSerialized;
import utils.DeserializationException;
import utils.Deserializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;

public class ECDSAPrivateKey implements CanBeSerialized {
    public static final Deserializer<ECDSAPrivateKey> DESERIALIZER = new ECDSAPrivateKeyDeserializer();
    public final BigInteger d;

    public ECDSAPrivateKey(BigInteger d) {
        this.d = d;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ECDSAPrivateKey)) {
            return false;
        }
        ECDSAPrivateKey other = (ECDSAPrivateKey) o;
        return d.equals(other.d);
    }

    @Override
    public int hashCode() {
        return d.hashCode();
    }

    @Override
    public void serialize(DataOutputStream outputStream) throws IOException {
        CanBeSerialized.serializeUnsignedBigInteger(outputStream, d, Crypto.ECDSA_ORDER_IN_BYTES);
    }

    private static class ECDSAPrivateKeyDeserializer implements Deserializer<ECDSAPrivateKey> {
        @Override
        public ECDSAPrivateKey deserialize(DataInputStream inputStream) throws DeserializationException, IOException {
            BigInteger d = Deserializer.deserializeUnsignedBigInteger(inputStream, Crypto.ECDSA_ORDER_IN_BYTES);
            return new ECDSAPrivateKey(d);
        }
    }
}

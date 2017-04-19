package crypto;

import utils.CanBeSerialized;
import utils.DeserializationException;
import utils.Deserializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Objects;

public final class ECDSASignature implements CanBeSerialized {
    public static final Deserializer<ECDSASignature> DESERIALIZER = new ECDSASignatureDeserializer();

    public final BigInteger r;
    public final BigInteger s;

    public ECDSASignature(BigInteger r, BigInteger s) {
        this.r = r;
        this.s = s;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ECDSASignature)) {
            return false;
        }
        ECDSASignature other = (ECDSASignature) o;
        return r.equals(other.r) && s.equals(other.s);
    }

    @Override
    public int hashCode() {
        return Objects.hash(r, s);
    }

    @Override
    public void serialize(DataOutputStream outputStream) throws IOException {
        CanBeSerialized.serializeUnsignedBigInteger(outputStream, r, Crypto.ECDSA_ORDER_IN_BYTES);
        CanBeSerialized.serializeUnsignedBigInteger(outputStream, s, Crypto.ECDSA_ORDER_IN_BYTES);
    }

    private static class ECDSASignatureDeserializer implements Deserializer<ECDSASignature> {
        @Override
        public ECDSASignature deserialize(DataInputStream inputStream) throws DeserializationException, IOException {
            BigInteger r = Deserializer.deserializeUnsignedBigInteger(inputStream, Crypto.ECDSA_ORDER_IN_BYTES);
            BigInteger s = Deserializer.deserializeUnsignedBigInteger(inputStream, Crypto.ECDSA_ORDER_IN_BYTES);
            return new ECDSASignature(r, s);
        }
    }
}

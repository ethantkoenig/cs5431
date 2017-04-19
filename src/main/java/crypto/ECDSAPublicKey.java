package crypto;

import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;
import utils.CanBeSerialized;
import utils.DeserializationException;
import utils.Deserializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;

public final class ECDSAPublicKey implements CanBeSerialized {
    public static final Deserializer<ECDSAPublicKey> DESERIALIZER = new ECDSAPublicKeyDeserializer();

    public final ECPoint point;

    public ECDSAPublicKey(ECPoint point) {
        this.point = point;
    }

    public ECDSAPublicKey(BigInteger x, BigInteger y) {
        ECCurve curve = Crypto.SPEC.getCurve();
        this.point = curve.createPoint(x, y);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ECDSAPublicKey)) {
            return false;
        }
        ECDSAPublicKey other = (ECDSAPublicKey) o;
        return point.equals(other.point);
    }

    @Override
    public int hashCode() {
        return point.hashCode();
    }

    @Override
    public void serialize(DataOutputStream outputStream) throws IOException {
        BigInteger x = point.getXCoord().toBigInteger();
        CanBeSerialized.serializeUnsignedBigInteger(outputStream, x, Crypto.ECDSA_ORDER_IN_BYTES);
        BigInteger y = point.getYCoord().toBigInteger();
        CanBeSerialized.serializeUnsignedBigInteger(outputStream, y, Crypto.ECDSA_ORDER_IN_BYTES);
    }

    private static class ECDSAPublicKeyDeserializer implements Deserializer<ECDSAPublicKey> {
        @Override
        public ECDSAPublicKey deserialize(DataInputStream inputStream)
                throws DeserializationException, IOException {
            BigInteger x = Deserializer.deserializeUnsignedBigInteger(inputStream, Crypto.ECDSA_ORDER_IN_BYTES);
            BigInteger y = Deserializer.deserializeUnsignedBigInteger(inputStream, Crypto.ECDSA_ORDER_IN_BYTES);
            return new ECDSAPublicKey(x, y);
        }
    }
}

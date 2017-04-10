package transaction;

import crypto.ECDSAPublicKey;
import utils.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;


/**
 * Transaction output class.
 * Contains the value associated with this output and the public key
 * required to claim this output.
 */
public final class TxOut implements CanBeSerialized {

    public final static Deserializer<TxOut> DESERIALIZER = new TxOutDeserializer();

    public final long value;
    public final ECDSAPublicKey ownerPubKey;

//     private transient Optional<byte[]> encodedPubKeyCache = Optional.empty();

    public TxOut(long value, ECDSAPublicKey ownerPubKey) {
        this.value = value;
        this.ownerPubKey = ownerPubKey;
    }

    public void serialize(DataOutputStream outputStream) throws IOException {
//        byte[] encodedPubKey;
//        if (encodedPubKeyCache.isPresent()) {
//            encodedPubKey = encodedPubKeyCache.get();
//        } else {
//            encodedPubKey = ;
//            encodedPubKeyCache = Optional.of(encodedPubKey);
//        }
        outputStream.write(ByteUtil.asByteArray(ownerPubKey::serialize));
        outputStream.writeLong(value);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof TxOut)) {
            return false;
        }
        TxOut other = (TxOut) o;
        return ((this.value == other.value) && (this.ownerPubKey.equals(other.ownerPubKey)));
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{value, ownerPubKey});
    }

    private static final class TxOutDeserializer implements Deserializer<TxOut> {
        @Override
        public TxOut deserialize(DataInputStream inputStream) throws DeserializationException, IOException {
            ECDSAPublicKey ownerKey = ECDSAPublicKey.DESERIALIZER.deserialize(inputStream);
            long value = inputStream.readLong();
            return new TxOut(value, ownerKey);
        }
    }
}

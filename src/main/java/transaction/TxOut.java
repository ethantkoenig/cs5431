package transaction;

import utils.Crypto;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Optional;


/**
 * Transaction output class.
 * Contains the value associated with this output and the public key
 * required to claim this output.
 */
public class TxOut {

    public final long value;
    public final PublicKey ownerPubKey;

    private transient Optional<byte[]> encodedPubKeyCache = Optional.empty();

    public TxOut(long value, PublicKey ownerPubKey) {
        this.value = value;
        this.ownerPubKey = ownerPubKey;
    }

    public static TxOut deserialize(DataInputStream input)
            throws GeneralSecurityException, IOException {
        PublicKey ownerKey = Crypto.deserializePublicKey(input);
        long value = input.readLong();
        return new TxOut(value, ownerKey);
    }

    public void serialize(DataOutputStream outputStream) throws IOException {
        byte[] encodedPubKey;
        if (encodedPubKeyCache.isPresent()) {
            encodedPubKey = encodedPubKeyCache.get();
        } else {
            encodedPubKey = ownerPubKey.getEncoded();
            encodedPubKeyCache = Optional.of(encodedPubKey);
        }
        outputStream.write(encodedPubKey);
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
}

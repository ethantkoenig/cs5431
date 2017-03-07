package transaction;

import utils.Crypto;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Arrays;


/**
 * Transaction output class.
 * Contains the value associated with this output and the public key
 * required to claim this output.
 */
public class RTxOut {

    public final long value;
    public final PublicKey ownerPubKey;

    public RTxOut(long value, PublicKey ownerPubKey) {
        this.value = value;
        this.ownerPubKey = ownerPubKey;
    }

    public static RTxOut deserialize(ByteBuffer input) throws GeneralSecurityException {
        PublicKey ownerKey = Crypto.deserializePublicKey(input);
        long value = input.getLong();
        return new RTxOut(value, ownerKey);
    }

    public void serialize(DataOutputStream outputStream) throws IOException {
        outputStream.write(ownerPubKey.getEncoded());
        outputStream.writeLong(value);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o == null || !(o instanceof RTxOut)) {
            return false;
        }
        RTxOut other = (RTxOut) o;
        return ((this.value == other.value) && (this.ownerPubKey.equals(other.ownerPubKey)));
    }

    @Override
    public int hashCode() {return Arrays.hashCode(new Object[] { value, ownerPubKey });}
}

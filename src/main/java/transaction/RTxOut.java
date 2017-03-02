package transaction;

import utils.Crypto;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.PublicKey;


/**
 * Transaction output class. Contains the value associated with this output
 * and the public key script required to claim this output.
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
}

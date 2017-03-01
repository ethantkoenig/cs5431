package transaction;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;

/**
 * Created by willronchetti on 2/22/17.
 */

/**
 * Transaction output class. Contains the value associated with this output
 * and the public key script required to claim this output.
 */
public class RTxOut {

    private long value;
    private byte[] ownerPubKey;

    /** Public constructor for Output object. Sets default fields and allocates memory
     *
     */
    public RTxOut() {
        value = 0;
        ownerPubKey = new byte[RSignature.PUBLIC_KEY_SIZE];
    }

    /** Sets the coin value associated with this transaction output.
     *
     * @param val is the number of coins.
     */
    public void setValue(long val){
        assert (val > 0);
        value = val;
    }

    /** Sets the public key script to be the pubkey given
     * @param pubkey is the public key of the new owner of the coins.
     * @throws GeneralSecurityException
     */
    public void setScriptpubkey(byte[] pubkey) throws GeneralSecurityException {
        assert (pubkey.length == RSignature.PUBLIC_KEY_SIZE);
        ownerPubKey = pubkey.clone();

    }
}

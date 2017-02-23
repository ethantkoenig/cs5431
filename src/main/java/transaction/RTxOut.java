import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;

import utils.Crypto;

/**
 * Created by willronchetti on 2/22/17.
 */

/**
 * Transaction output class. Contains the value associated with this output
 * and the public key script required to claim this output.
 */
public class RTxOut {
    int value;
    ByteBuffer scriptpubkey;

    public void setValue(int val){
        assert val > 0;
        value = val;
    }

//  Sets the public key script to be the pubkey given if already hashed or
//  hashes the public key given to get the proper script.
    public void setScriptpubkey(ByteBuffer pubkey) throws GeneralSecurityException {
        assert pubkey.capacity() == 32 || pubkey.capacity() == 91;
        if (pubkey.capacity() == 32) {
            scriptpubkey = pubkey;
        }
        else {
            scriptpubkey = ByteBuffer.wrap(Crypto.sha256(pubkey.array()));
        }
    }
}

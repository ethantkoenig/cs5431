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
    int value;
    ByteBuffer scriptPubKey;

    public void setValue(int val){
        assert val > 0;
        value = val;
    }

//  Sets the public key script to be the pubkey given
//  Note that we are sticking with straight up public key for now.
    public void setScriptpubkey(ByteBuffer pubkey) throws GeneralSecurityException {
        assert pubkey.capacity() == 91;
        scriptPubKey = ByteBuffer.wrap(pubkey.array());

    }
}

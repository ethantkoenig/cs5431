package transaction;

import utils.ShaTwoFiftySix;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * Class representing the TxID, input index and Script triple.
 * TxID is a hash corresponding to a previous transaction to be referenced
 * in the new transaction. Input idex is the output number of the previous
 * transaction. The script (for now) is the SHA256 hash of the public key
 * owning the funds, which will be signed using the associated Private Key
 */
public class RTxIn {

    public final ShaTwoFiftySix previousTxn;
    public final int txIdx;

    public RTxIn(ShaTwoFiftySix previousTxn, int index) {
        this.previousTxn = previousTxn;
        txIdx = index;
    }

    public static RTxIn deserialize(ByteBuffer input) {
        ShaTwoFiftySix sha = ShaTwoFiftySix.deserialize(input);
        int index = input.getInt();
        return new RTxIn(sha, index);
    }

    public void serialize(DataOutputStream outputStream) throws IOException {
        previousTxn.writeTo(outputStream);
        outputStream.writeInt(txIdx);
    }
}

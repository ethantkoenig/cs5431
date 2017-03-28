package transaction;

import utils.ShaTwoFiftySix;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;


/**
 * Transaction input class.
 * Contains a reference to a previous transactions output in the form
 * of a SHA256 hash of the transaction, and the output index to be spent.
 */
public final class TxIn {

    public final ShaTwoFiftySix previousTxn;
    public final int txIdx;

    public TxIn(ShaTwoFiftySix previousTxn, int index) {
        this.previousTxn = previousTxn;
        txIdx = index;
    }

    public static TxIn deserialize(DataInputStream input) throws IOException {
        ShaTwoFiftySix sha = ShaTwoFiftySix.deserialize(input);
        int index = input.readInt();
        return new TxIn(sha, index);
    }

    public void serialize(DataOutputStream outputStream) throws IOException {
        previousTxn.writeTo(outputStream);
        outputStream.writeInt(txIdx);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof TxIn)) {
            return false;
        }
        TxIn other = (TxIn) o;
        return (other.previousTxn.equals(this.previousTxn)) && (this.txIdx == other.txIdx);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{previousTxn, txIdx});
    }
}

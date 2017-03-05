package transaction;


import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import block.UnspentTransactions;
import utils.ShaTwoFiftySix;
import utils.Pair;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Main transaction class. Keeps track of several pieces of information key to the
 * transaction, such as the number of inputs, and outputs, the inputs and outputs
 * themselves, and some additional bookkeeping for proper insertion of input and
 * output classes
 */
public class RTransaction {

    private final RTxIn[] txIn;
    private final RTxOut[] txOut;
    private final RSignature[] signatures;

    private RTransaction(RTxIn[] txIn, RTxOut[] txOut, RSignature[] signatures) {
        this.txIn = txIn;
        this.txOut = txOut;
        this.signatures = signatures;
    }

    private static RTransaction createAndSign(RTxIn[] inputs, RTxOut[] outputs, PrivateKey[] keys)
            throws IOException, GeneralSecurityException {
        if (inputs.length != keys.length) {
            throw new IllegalStateException();
        }
        RTransaction txn = new RTransaction(inputs, outputs, null);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        txn.serialize(new DataOutputStream(outputStream));
        byte[] body = outputStream.toByteArray();

        RSignature[] signatures = new RSignature[keys.length];
        for (int i = 0; i < keys.length; i++) {
            signatures[i] = RSignature.sign(body, keys[i]);
        }

        return new RTransaction(inputs, outputs, signatures);
    }

    public static RTransaction deserialize(ByteBuffer input) throws GeneralSecurityException {
        final int numInputs = input.getInt();
        RTxIn[] inputs = new RTxIn[numInputs];
        for (int i = 0; i < numInputs; i++) {
            inputs[i] = RTxIn.deserialize(input);
        }

        final int numOutputs = input.getInt();
        RTxOut[] outputs = new RTxOut[numOutputs];
        for (int i = 0; i < numOutputs; i++) {
            outputs[i] = RTxOut.deserialize(input);
        }
        RSignature[] signatures = new RSignature[numInputs];
        for (int i = 0; i < numInputs; i++) {
            signatures[i] = RSignature.deserialize(input);
        }
        return new RTransaction(inputs, outputs, signatures);
    }

    /**
     * Serialize this transaction (excluding signatures), and write it to {@code outputStream}
     *
     * @param outputStream output to write serialized transaction to
     * @throws IOException
     */
    public void serialize(DataOutputStream outputStream) throws IOException {
        outputStream.writeInt(txIn.length);
        for (RTxIn input : txIn) {
            input.serialize(outputStream);
        }

        outputStream.writeInt(txOut.length);
        for (RTxOut output : txOut) {
            output.serialize(outputStream);
        }
    }

    /**
     * Serialize this transaction (including signatures), and write it to {@code outputStream}
     *
     * @param outputStream output to write serialized transaction to
     * @throws IOException
     */
    public void serializeWithSignatures(DataOutputStream outputStream) throws IOException {
        if (signatures == null) {
            throw new IllegalStateException("Cannot fully serialize unsigned transaction");
        }
        serialize(outputStream);
        for (RSignature signature : signatures) {
            signature.serialize(outputStream);
        }
    }

    /**
     * Verify the signature corresponding to the {@code inputIndex}^th input.
     *
     * @param inputIndex index of input to verify
     * @param key        public key to use for verification, should match the public key of the output
     *                   corresponding to the relevant input
     * @return whether the signature was verified
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public boolean verifySignature(int inputIndex, PublicKey key)
            throws GeneralSecurityException, IOException {
        if (signatures == null) {
            throw new IllegalStateException("Cannot verify unsigned transaction");
        } else if (inputIndex < 0 || inputIndex >= signatures.length) {
            String msg = String.format("Invalid index: %d, expected to be in range [0, %d)",
                    inputIndex, txIn.length);
            throw new IllegalArgumentException(msg);
        }

        // TODO: eventually find a way to not re-serialize every time
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        serialize(new DataOutputStream(outputStream));
        return signatures[inputIndex].verify(outputStream.toByteArray(), key);
    }

    /**
     * Verifies {@code this RTransaction} with respect to {@code unspentOutputs}.
     *
     * This will check that all signatures are valid and that there are no double spends.
     *
     * @param unspentOutputs A {@code Map} containing all of the unspent outputs that {@code this} may refer to. Entries
     *                       that are spent by {@code this RTransaction} will be removed. If verification fails, there
     *                       are no guarantees as to the state of this {@code Map}.
     * @return Whether this {@code RTransaction} was successfully verified.
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public boolean verify(UnspentTransactions unspentOutputs)
            throws GeneralSecurityException, IOException {
        for(int i = 0; i < txIn.length; ++i) {
            RTxIn in = txIn[i];
            if (!unspentOutputs.contains(in.previousTxn, in.txIdx)) {
                return false;
            }
            RTxOut out = unspentOutputs.remove(in.previousTxn, in.txIdx);
            if (!verifySignature(i, out.ownerPubKey)) {
                return false;
            }
        }
        return true;
    }

    public static class Builder {
        List<RTxIn> inputs = new ArrayList<>();
        List<PrivateKey> privateKeys = new ArrayList<>();
        List<RTxOut> outputs = new ArrayList<>();

        public Builder addInput(RTxIn input, PrivateKey key) {
            inputs.add(input);
            privateKeys.add(key);
            return this;
        }

        public Builder addOutput(RTxOut output) {
            outputs.add(output);
            return this;
        }

        public RTransaction build() throws IOException, GeneralSecurityException {
            return RTransaction.createAndSign(
                    inputs.toArray(new RTxIn[inputs.size()]),
                    outputs.toArray(new RTxOut[outputs.size()]),
                    privateKeys.toArray(new PrivateKey[privateKeys.size()])
            );
        }
    }
}

package block;

import transaction.Transaction;
import transaction.TxOut;
import utils.CanBeSerialized;
import utils.HashCache;
import utils.ShaTwoFiftySix;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class MinedBlock extends HashCache implements CanBeSerialized {

    public final TxOut reward;
    public final Transaction[] transactions;
    public final ShaTwoFiftySix previousBlockHash;
    public final byte[] nonce;

    public MinedBlock(MiningBlock block) {
        reward = block.reward;
        transactions = Arrays.copyOf(block.transactions, block.transactions.length);
        previousBlockHash = block.previousBlockHash;
        nonce = Arrays.copyOf(block.nonce, block.nonce.length);
        if (nonce.length != MiningBlock.NONCE_SIZE_IN_BYTES) {
            throw new IllegalStateException("Incorrect nonce size");
        }
        for (Transaction transaction : transactions) {
            if (transaction == null) {
                throw new IllegalStateException("Cannot construct from a non-full MiningBlock");
            }
        }
        if (reward == null) {
            throw new IllegalStateException("Cannot construct a MinedBlock without a reward");
        }
    }
}

package block;

import utils.ShaTwoFiftySix;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Represents a block of transactions in the ledger
 */
public class Block {
    public final static int NUM_TRANSACTIONS_PER_BLOCK = 128;
    public final static int NONCE_SIZE_IN_BYTES = 128;

    public final ShaTwoFiftySix previousBlockHash;
    public final Transaction[] transactions = new Transaction[NUM_TRANSACTIONS_PER_BLOCK];
    public final byte[] nonce = new byte[NONCE_SIZE_IN_BYTES];

    private Block(ShaTwoFiftySix previousBlockHash) {
        this.previousBlockHash = previousBlockHash;
    }

    public static Block empty(ShaTwoFiftySix previousBlockHash) {
        return new Block(previousBlockHash);
    }

    public static Block deserialize(ByteBuffer input) {
        byte[] previousBlockHash = new byte[ShaTwoFiftySix.HASH_SIZE_IN_BYTES];
        input.get(previousBlockHash);
        Block block = new Block(ShaTwoFiftySix.sha256(previousBlockHash));
        for (int i = 0; i < NUM_TRANSACTIONS_PER_BLOCK; i++) {
            block.transactions[i] = Transaction.deserialize(input);
        }
        input.get(block.nonce);
        return block;
    }

    public void serialize(OutputStream outputStream) throws IOException {
        previousBlockHash.writeTo(outputStream);
        for (Transaction transaction : transactions) {
            transaction.serialize(outputStream);
        }
        outputStream.write(nonce);
    }

    // TODO a placeholder for the real Transaction class
    public static class Transaction {
        public static Transaction deserialize(ByteBuffer input) {
            return null;
        }

        public void serialize(OutputStream outputStream) {
        }
    }

}

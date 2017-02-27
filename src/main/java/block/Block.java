package block;

import utils.ByteUtil;
import utils.ShaTwoFiftySix;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

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

    /**
     * @param previousBlockHash SHA-256 hash of the previous Block
     * @return an empty block.
     */
    public static Block empty(ShaTwoFiftySix previousBlockHash) {
        return new Block(previousBlockHash);
    }

    /**
     * @param input input bytes to deserialize
     * @return deserialized block
     * @throws BufferUnderflowException if the buffer is too short
     */
    public static Block deserialize(ByteBuffer input) throws BufferUnderflowException {
        Block block = new Block(ShaTwoFiftySix.deserialize(input));
        for (int i = 0; i < NUM_TRANSACTIONS_PER_BLOCK; i++) {
            block.transactions[i] = Transaction.deserialize(input);
        }
        input.get(block.nonce);
        return block;
    }

    /**
     * Writes the serialization of this block to {@code outputStream}
     * @param outputStream {@code OutputStream} to write the serialized block to
     * @throws IOException
     */
    public void serialize(OutputStream outputStream) throws IOException {
        previousBlockHash.writeTo(outputStream);
        for (Transaction transaction : transactions) {
            transaction.serialize(outputStream);
        }
        outputStream.write(nonce);
    }

    public void nonceAddOne() throws Exception {
        ByteUtil.addOne(this.nonce);
    }

    /**
     * @return The SHA-256 hash of the serialization of {@code this}
     */
    public ShaTwoFiftySix getShaTwoFiftySix() {
        // TODO: hook together serialization and util.ShaTwoFiftySix
        return null;
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
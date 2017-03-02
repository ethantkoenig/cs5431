package block;

import utils.ByteUtil;
import utils.ShaTwoFiftySix;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Represents a block of transactions in the ledger
 */
public class Block {
    private final static Logger LOGGER = Logger.getLogger(Block.class.getName());

    public final static int NUM_TRANSACTIONS_PER_BLOCK = 4;
    public final static int NONCE_SIZE_IN_BYTES = 128;

    public final ShaTwoFiftySix previousBlockHash;
    public final Transaction[] transactions = new Transaction[NUM_TRANSACTIONS_PER_BLOCK];
    public final byte[] nonce = new byte[NONCE_SIZE_IN_BYTES];

    private int hashGoalZeros = 2;

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
     *
     * @param outputStream {@code OutputStream} to write the serialized block to
     * @throws IOException
     */
    public void serialize(OutputStream outputStream) throws IOException {
        previousBlockHash.writeTo(outputStream);
        for (Transaction transaction : transactions) {
            if (transaction != null)
                transaction.serialize(outputStream);
        }
        outputStream.write(nonce);
    }

    public void nonceAddOne() throws Exception {
        ByteUtil.addOne(this.nonce);
    }

    public boolean checkHash() throws IOException {
        ShaTwoFiftySix hash = null;
        byte[] bytes = null;

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        this.serialize(os);
        try {
            bytes = ByteUtil.concatenate(os.toByteArray(), nonce);
            hash = ShaTwoFiftySix.hashOf(bytes);
        } catch (GeneralSecurityException e) {
            LOGGER.severe("Unable to hash: " + Arrays.toString(bytes));
            e.printStackTrace();
        }
        return (hash != null) ? hash.checkHashZeros(hashGoalZeros): false;
    }

    /**
     * @return The SHA-256 hash of the serialization of {@code this}
     */
    public ShaTwoFiftySix getShaTwoFiftySix() {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            serialize(outputStream);
            return ShaTwoFiftySix.hashOf(outputStream.toByteArray());
        } catch (IOException | GeneralSecurityException e) {
            LOGGER.severe(e.getMessage());
            throw new RuntimeException(e);
        }
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
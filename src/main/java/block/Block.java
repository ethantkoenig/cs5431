package block;


import utils.ByteUtil;

import transaction.RTransaction;
import transaction.RTxOut;
import utils.Pair;
import utils.ShaTwoFiftySix;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;

import java.util.Arrays;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import java.util.logging.Logger;

/**
 * Represents a block of transactions in the ledger
 */
public class Block {
    private final static Logger LOGGER = Logger.getLogger(Block.class.getName());

    public final static int NUM_TRANSACTIONS_PER_BLOCK = 4;
    public final static int NONCE_SIZE_IN_BYTES = 128;

    public final ShaTwoFiftySix previousBlockHash;
    public final RTransaction[] transactions = new RTransaction[NUM_TRANSACTIONS_PER_BLOCK];
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
    public static Block deserialize(ByteBuffer input)
            throws BufferUnderflowException, GeneralSecurityException {
        Block block = new Block(ShaTwoFiftySix.deserialize(input));
        for (int i = 0; i < NUM_TRANSACTIONS_PER_BLOCK; i++) {
            block.transactions[i] = RTransaction.deserialize(input);
        }
        input.get(block.nonce);
        return block;
    }

    /**
     * Writes the serialization of this block to {@code outputStream}
     *
     * @param outputStream output to write the serialized block to
     * @throws IOException
     */
    public void serialize(DataOutputStream outputStream) throws IOException {
        previousBlockHash.writeTo(outputStream);

        for (RTransaction transaction : transactions) {
            if (transaction != null)
                transaction.serialize(outputStream);

        }
        outputStream.write(nonce);
    }

    public void nonceAddOne() throws Exception {
        ByteUtil.addOne(this.nonce);
    }

    public boolean checkHash() throws IOException, GeneralSecurityException {
        ShaTwoFiftySix hash = null;
        byte[] bytes = null;

        ByteArrayOutputStream os = new ByteArrayOutputStream();
//        this.serialize(os);
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
            serialize(new DataOutputStream(outputStream));
            return ShaTwoFiftySix.hashOf(outputStream.toByteArray());
        } catch (IOException | GeneralSecurityException e) {
            LOGGER.severe(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Verifies the validity of {@code this} with respect to {@code unspentTransactions}.
     *
     * A {@code Block} is said to be valid with respect to a set of unspent transactions if its inputs only contain
     * outputs from that set, it contains no double spends, and every input has a valid signature.
     *
     * @param unspentTransactions A list of unspent {@code RTransaction}s that may be spent by {@code this Block}. This
     *                            collection will not be modified.
     * @return The new {@code Map} with spent transactions removed, if verification passes. Otherwise {@code Optional.empty()}.
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public Optional<HashMap<Pair<ShaTwoFiftySix,Integer>,RTxOut>>
    verifyBlock(Map<Pair<ShaTwoFiftySix,Integer>, RTxOut> unspentTransactions)
            throws GeneralSecurityException, IOException {
        HashMap<Pair<ShaTwoFiftySix,Integer>, RTxOut> workingTxs = new HashMap<>(unspentTransactions);

        for (RTransaction tx: transactions) {
            if (!tx.verify(workingTxs)) {
                return Optional.empty();
            }
        }
        return Optional.of(workingTxs);
    }

}


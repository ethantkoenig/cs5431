package block;


import transaction.Transaction;
import transaction.TxOut;
import utils.ByteUtil;
import utils.Config;
import utils.Crypto;
import utils.IOUtils;
import utils.ShaTwoFiftySix;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.*;
import java.util.logging.Logger;
import java.io.ByteArrayOutputStream;

/**
 * Represents a block of transactions in the ledger
 */
public class Block implements Iterable<Transaction> {
    private final static Logger LOGGER = Logger.getLogger(Block.class.getName());

    public final static int NUM_TRANSACTIONS_PER_BLOCK = 2;
    public final static int NONCE_SIZE_IN_BYTES = 128;
    public final static int REWARD_AMOUNT = 50000;

    public final ShaTwoFiftySix previousBlockHash;
    public final Transaction[] transactions;
    public TxOut reward;
    public final byte[] nonce = new byte[NONCE_SIZE_IN_BYTES];

    private Block(ShaTwoFiftySix previousBlockHash, int numTransactions) {
        this.previousBlockHash = previousBlockHash;
        this.transactions = new Transaction[numTransactions];
    }

    /**
     * @return a genesis block
     */
    public static Block genesis() {
        return new Block(ShaTwoFiftySix.zero(), 0);
    }

    /**
     * @param previousBlockHash SHA-256 hash of the previous Block
     * @return an empty block.
     */
    public static Block empty(ShaTwoFiftySix previousBlockHash) {
        return new Block(previousBlockHash, NUM_TRANSACTIONS_PER_BLOCK);
    }

    /**
     * @param input input bytes to deserialize
     * @return Array of deserialized blocks
     */
    public static Block[] deserializeBlocks(byte[] input)
            throws IOException, GeneralSecurityException {
        return deserializeBlocks(new DataInputStream(new ByteArrayInputStream(input)));
    }

    /**
     * @param input input bytes to deserialize
     * @return Array of deserialized blocks
     */
    public static Block[] deserializeBlocks(DataInputStream input)
            throws IOException, GeneralSecurityException {
        int numBlocks = input.readInt();
        Block[] blocks = new Block[numBlocks];
        for (int i = 0; i < numBlocks; ++i) {
            blocks[i] = Block.deserialize(input);
        }
        return blocks;
    }

    /**
     * @param input input bytes to deserialize
     * @return deserialized block
     */
    public static Block deserialize(byte[] input) throws IOException, GeneralSecurityException {
        return deserialize(new DataInputStream(new ByteArrayInputStream(input)));
    }

    /**
     * @param input input bytes to deserialize
     * @return deserialized block
     */
    public static Block deserialize(DataInputStream input)
            throws IOException, GeneralSecurityException {
        ShaTwoFiftySix hash = ShaTwoFiftySix.deserialize(input);
        int numBlocks = input.readInt();
        Block block = new Block(hash, numBlocks);

        for (int i = 0; i < numBlocks; i++) {
            block.transactions[i] = Transaction.deserialize(input);
        }
        PublicKey rewardKey = Crypto.deserializePublicKey(input);
        block.reward = new TxOut(REWARD_AMOUNT, rewardKey);
        IOUtils.fill(input, block.nonce);
        return block;
    }

    public static byte[] serializeBlocks(List<Block> blocks) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(outputStream);
        dataOut.writeInt(blocks.size());
        for (Block b : blocks) {
            b.serialize(dataOut);
        }
        return outputStream.toByteArray();
    }

    /**
     * Writes the serialization of this block to {@code outputStream}
     *
     * @param outputStream output to write the serialized block to
     * @throws IOException
     */
    public void serialize(DataOutputStream outputStream) throws IOException {
        previousBlockHash.writeTo(outputStream);
        outputStream.writeInt(transactions.length);

        for (Transaction transaction : transactions) {
            if (transaction == null) {
                throw new IllegalStateException("Cannot serialize non-full block");
            }
            transaction.serializeWithSignatures(outputStream);
        }
        if (reward == null) {
            throw new IllegalStateException("Cannot serialize block without reward");
        }
        outputStream.write(reward.ownerPubKey.getEncoded());
        outputStream.write(nonce);
    }

    /**
     * Add one to the nonce byte array
     */
    public void nonceAddOne() throws Exception {
        ByteUtil.addOne(this.nonce);
    }

    /**
     * Set the nonce to a random value
     */
    public void setRandomNonce(Random random) {
        random.nextBytes(nonce);
    }

    /**
     * Check that hashing the block with the current nonce does in fact result in a hash
     * with hashGoalZeros number of leading zeros.
     */
    public boolean checkHash() throws IOException, GeneralSecurityException {
        return checkHashWith(Config.HASH_GOAL.get());
    }

    /* package */ boolean checkHashWith(int goal) throws IOException, GeneralSecurityException {
        ShaTwoFiftySix hash = ShaTwoFiftySix.hashOf(ByteUtil.asByteArray(this::serialize));
        return hash.checkHashZeros(goal);
    }

    /**
     * @return Whether {@code this} is a genesis {@code Block}
     */
    public boolean isGenesisBlock() {
        return previousBlockHash.equals(ShaTwoFiftySix.zero());
    }

    /**
     * @return The SHA-256 hash of the serialization of {@code this}
     */
    public ShaTwoFiftySix getShaTwoFiftySix() {
        try {
            return ShaTwoFiftySix.hashOf(ByteUtil.asByteArray(this::serialize));
        } catch (IOException | GeneralSecurityException e) {
            LOGGER.severe(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * @return true if block has all NUM_TRANSACTIONS_PER_BLOCK filled
     */
    public boolean isFull() {
        for (Transaction transaction : transactions) {
            if (transaction == null)
                return false;
        }
        return true;
    }

    /**
     * Add a transaction to the block
     *
     * @param newTransaction the transaction to be added
     * @return true if successful
     */
    public boolean addTransaction(Transaction newTransaction) {
        if (this.isFull()) return false;
        for (int i = 0; i < transactions.length; i++) {
            if (transactions[i] == null) {
                transactions[i] = newTransaction;
                return true;
            }
        }
        // Should never get here
        return false;
    }

    /**
     * Returns the list of transactions that are in block other but not in this block
     *
     * @param other the block that we are comparing against
     * @return ArrayList the list of transactions that are in block other but not in this block
     */
    public ArrayList<Transaction> getTransactionDifferences(Block other) {
        HashSet<Transaction> transSet = new HashSet<>();
        for (Transaction thisTransaction : transactions) {
            transSet.add(thisTransaction);
        }

        ArrayList<Transaction> result = new ArrayList<>();
        for (Transaction otherTransaction : other.transactions) {
            if (!transSet.contains(otherTransaction)) {
                result.add(otherTransaction);
            }
        }
        return result;
    }

    /*
     * Add a reward transaction
     */
    public void addReward(PublicKey publicKey) {
        if (reward != null) {
            throw new IllegalStateException("Cannot reset block's reward");
        }
        reward = new TxOut(REWARD_AMOUNT, publicKey);
    }

    /**
     * Verifies the validity of {@code this} with respect to {@code unspentTransactions}.
     * <p>
     * A {@code Block} is said to be valid with respect to a set of unspent transactions if its inputs only contain
     * outputs from that set, it contains no double spends, and every input has a valid signature.
     *
     * @param unspentTransactions A list of unspent {@code Transaction}s that may be spent by {@code this Block}. This
     *                            collection will not be modified.
     * @return The new {@code Map} with spent transactions removed, if verification passes. Otherwise {@code Optional.empty()}.
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public Optional<UnspentTransactions> verify(UnspentTransactions unspentTransactions)
            throws GeneralSecurityException, IOException {
        UnspentTransactions copy = unspentTransactions.copy();

        for (Transaction tx : transactions) {
            if (!tx.verify(copy)) {
                return Optional.empty();
            }
        }
        if (this.reward.value != REWARD_AMOUNT) {
            return Optional.empty();
        }
        return Optional.of(copy);
    }

    public boolean verifyGenesis(PublicKey privilegedKey) {
        if (this.transactions.length > 0) {
            return false;
        }
        if (this.reward.value != REWARD_AMOUNT) {
            return false;
        }
        return this.reward.ownerPubKey.equals(privilegedKey);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Block) {
            Block b = (Block) other;
            // Relying on collision resistance of SHA-256 to check for equality
            return getShaTwoFiftySix().equals(b.getShaTwoFiftySix());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getShaTwoFiftySix().hashCode();
    }

    @Override
    public Iterator<Transaction> iterator() {
        return Arrays.stream(transactions).iterator();
    }
}


package block;


import org.bouncycastle.crypto.digests.SHA256Digest;
import transaction.Transaction;
import transaction.TxOut;
import utils.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Represents a block of transactions in the ledger
 */
public class MiningBlock implements CanBeSerialized {
    private final static Logger LOGGER = Logger.getLogger(MiningBlock.class.getName());
    public final static Deserializer<MiningBlock> DESERIALIZER = new BlockDeserializer();

    public final static int NUM_TRANSACTIONS_PER_BLOCK = 2;
    public final static int NONCE_SIZE_IN_BYTES = 128;
    public final static int REWARD_AMOUNT = 50000;

    public final ShaTwoFiftySix previousBlockHash;
    public final Transaction[] transactions;
    public TxOut reward;
    public final byte[] nonce;

    private MiningBlock(ShaTwoFiftySix previousBlockHash, Transaction[] transactions, TxOut reward) {
        this.previousBlockHash = previousBlockHash;
        this.transactions = transactions;
        this.reward = reward;
        this.nonce = new byte[NONCE_SIZE_IN_BYTES];
    }

    /**
     * Add one to the nonce byte array
     */
    public void nonceAddOne() {
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
    public boolean checkHash() {
        return getShaTwoFiftySix().checkHashZeros(Config.HASH_GOAL.get());
    }

    /**
     * @return Whether {@code this} is a genesis {@code MiningBlock}
     */
    public boolean isGenesisBlock() {
        return previousBlockHash.equals(ShaTwoFiftySix.zero());
    }

    @Override
    protected ShaTwoFiftySix computeHash() {
        try {
            return ShaTwoFiftySix.hashOf(ByteUtil.asByteArray(this::serialize));
        } catch (IOException | GeneralSecurityException e) {
            LOGGER.severe(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Update the `nonce` of `this` to make the SHA-256 hash have the correct number of zeros.
     *
     * @return Whether we finished finding a valid nonce
     */
    public boolean findValidNonce(AtomicBoolean quit) throws IOException {
        SHA256Digest digest = new SHA256Digest();
        byte[] ser = ByteUtil.asByteArray(this::serializeWithoutNonce);
        digest.update(ser, 0, ser.length);

        byte[] hash = new byte[ShaTwoFiftySix.HASH_SIZE_IN_BYTES];
        int hashGoal = Config.HASH_GOAL.get();

        do {
            if (quit.get()) {
                return false;
            }
            SHA256Digest copy = new SHA256Digest(digest);
            nonceAddOne();
            copy.update(nonce, 0, nonce.length);
            copy.doFinal(hash, 0);
        } while (!ShaTwoFiftySix.create(hash).get()
                .checkHashZeros(hashGoal));

        return true;
    }

    /**
     * Returns the list of transactions that are in block other but not in this block
     *
     * @param other the block that we are comparing against
     * @return ArrayList the list of transactions that are in block other but not in this block
     */
    public ArrayList<Transaction> getTransactionDifferences(MiningBlock other) {
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

    /**
     * Verifies the validity of {@code this} with respect to {@code unspentTransactions}.
     * <p>
     * A {@code MiningBlock} is said to be valid with respect to a set of unspent transactions if its inputs only contain
     * outputs from that set, it contains no double spends, and every input has a valid signature.
     *
     * @param unspentTransactions A list of unspent {@code Transaction}s that may be spent by {@code this MiningBlock}. This
     *                            collection will not be modified.
     * @return The new {@code Map} with spent transactions removed, if verification passes. Otherwise {@code Optional.empty()}.
     * @throws IOException
     */
    public Optional<UnspentTransactions> verify(UnspentTransactions unspentTransactions)
            throws IOException {
        if (!this.checkHash()) {
            return Optional.empty();
        } else if (this.reward.value != REWARD_AMOUNT) {
            return Optional.empty();
        }

        UnspentTransactions copy = unspentTransactions.copy();
        for (Transaction tx : transactions) {
            if (!tx.verify(copy)) {
                return Optional.empty();
            }
        }
        copy.put(getShaTwoFiftySix(), 0, reward);
        return Optional.of(copy);
    }

    public boolean verifyGenesis(PublicKey privilegedKey) {
        if (this.transactions.length > 0) {
            return false;
        } else if (this.reward.value != REWARD_AMOUNT) {
            return false;
        }
        return this.reward.ownerPubKey.equals(privilegedKey);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof MiningBlock) {
            MiningBlock b = (MiningBlock) other;
            // Relying on collision resistance of SHA-256 to check for equality
            return getShaTwoFiftySix().equals(b.getShaTwoFiftySix());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getShaTwoFiftySix().hashCode();
    }

    public ShaTwoFiftySix getShaTwoFiftySix() throws IOException, GeneralSecurityException {
        return ShaTwoFiftySix.hashOf(ByteUtil.asByteArray(this::serialize));
    }

    /**
     * Writes the serialization of this block to {@code outputStream}
     *
     * @param outputStream output to write the serialized block to
     * @throws IOException
     */
    @Override
    public void serialize(DataOutputStream outputStream) throws IOException {
        serializeWithoutNonce(outputStream);
        outputStream.write(nonce);
    }

    public void serializeWithoutNonce(DataOutputStream outputStream) throws IOException {
        previousBlockHash.writeTo(outputStream);
        CanBeSerialized.serializeArray(outputStream, transactions);
        outputStream.write(reward.ownerPubKey.getEncoded());
    }

    private static final class BlockDeserializer implements Deserializer<MiningBlock> {

        @Override
        public MiningBlock deserialize(DataInputStream input) throws DeserializationException, IOException {
            ShaTwoFiftySix hash = ShaTwoFiftySix.deserialize(input);

            List<Transaction> transactions = Deserializer.deserializeList(input, Transaction.DESERIALIZER);
            MiningBlock block = new MiningBlock(hash, transactions.size());
            for (int i = 0; i < transactions.size(); i++) {
                block.transactions[i] = transactions.get(i);
            }

            try {
                PublicKey rewardKey = Crypto.deserializePublicKey(input);
                block.reward = new TxOut(REWARD_AMOUNT, rewardKey);
                IOUtils.fill(input, block.nonce);
                return block;
            } catch (GeneralSecurityException e) {
                throw new DeserializationException("Misformatted reward public key");
            }
        }
    }

    public static final class BlockBuilder {
        private ShaTwoFiftySix previousBlockHash;
        private TxOut reward;
        private Transaction[] transactions;

        public BlockBuilder(ShaTwoFiftySix previousBlockHash) {
            this.previousBlockHash = previousBlockHash;
            transactions = new Transaction[MiningBlock.NUM_TRANSACTIONS_PER_BLOCK];
        }

        /**
         * @return a genesis BlockBuilder
         */
        public static BlockBuilder genesis() {
            return new BlockBuilder(ShaTwoFiftySix.zero());
        }


        public MiningBlock build() {

        }

        /**
         * Add a reward transaction
         */
        public BlockBuilder addReward(PublicKey publicKey) {
            reward = new TxOut(REWARD_AMOUNT, publicKey);
            return this;
        }

        /**
         * Add a transaction to the Block in progress
         *
         * @param newTransaction the transaction to be added
         * @return true if successful
         */
        public BlockBuilder addTransaction(Transaction newTransaction) {
            if (this.isFull()) {
                throw new IllegalStateException("Tried to add a Transaction to a full BlockBuilder");
            }
            for (int i = 0; i < transactions.length; i++) {
                if (transactions[i] == null) {
                    transactions[i] = newTransaction;
                    return this;
                }
            }
            // Should never get here
            return this;
        }

        /**
         * @return true if block has all `NUM_TRANSACTIONS_PER_BLOCK` filled
         */
        public boolean isFull() {
            for (Transaction transaction : transactions) {
                if (transaction == null)
                    return false;
            }
            return true;
        }
    }
}


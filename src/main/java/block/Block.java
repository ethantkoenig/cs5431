package block;


import crypto.ECDSAPublicKey;
import org.bouncycastle.crypto.digests.SHA256Digest;
import transaction.Transaction;
import transaction.TxOut;
import utils.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a block of transactions in the ledger
 */
public class Block extends HashCache implements Iterable<Transaction>, CanBeSerialized {
    private final static Log LOGGER = Log.forClass(Block.class);
    public final static Deserializer<Block> DESERIALIZER = new BlockDeserializer();

    public final static int NUM_TRANSACTIONS_PER_BLOCK = 2;
    public final static int NONCE_SIZE_IN_BYTES = 128;
    public final static int REWARD_AMOUNT = 50000;

    public final ShaTwoFiftySix previousBlockHash;
    public final Transaction[] transactions;
    public final TxOut reward;
    public final byte[] nonce = new byte[NONCE_SIZE_IN_BYTES];

    Block(ShaTwoFiftySix previousBlockHash, Transaction[] transactions, TxOut reward) {
        this.previousBlockHash = previousBlockHash;
        this.transactions = transactions;
        this.reward = reward;
    }

    /**
     * @return a genesis block
     */
    public static Block genesis(ECDSAPublicKey rewardKey) {
        return new Block(ShaTwoFiftySix.zero(), new Transaction[0], new TxOut(REWARD_AMOUNT, rewardKey));
    }

    public static Block block(ShaTwoFiftySix previousBlockHash,
                              List<Transaction> transactions,
                              ECDSAPublicKey rewardKey) {
        Transaction[] transactionsArray = transactions.toArray(new Transaction[transactions.size()]);
        return block(previousBlockHash, transactionsArray, rewardKey);
    }

    public static Block block(ShaTwoFiftySix previousBlockHash,
                              Transaction[] transactions,
                              ECDSAPublicKey rewardKey) {
        if (transactions.length != NUM_TRANSACTIONS_PER_BLOCK) {
            throw new IllegalArgumentException("Invalid number of transactions");
        }
        return new Block(previousBlockHash, transactions, new TxOut(REWARD_AMOUNT, rewardKey));
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
        reward.ownerPubKey.serialize(outputStream);
    }

    /**
     * Add one to the nonce byte array
     */
    public void nonceAddOne() {
        ByteUtil.addOne(this.nonce);
        invalidateCache();
    }

    /**
     * Set the nonce to a random value
     */
    public void setRandomNonce(Random random) {
        random.nextBytes(nonce);
        invalidateCache();
    }

    /**
     * Check that hashing the block with the current nonce does in fact result in a hash
     * with hashGoalZeros number of leading zeros.
     */
    public boolean checkHash() {
        return getShaTwoFiftySix().checkHashZeros(Config.hashGoal());
    }

    /**
     * @return Whether {@code this} is a genesis {@code Block}
     */
    public boolean isGenesisBlock() {
        return previousBlockHash.equals(ShaTwoFiftySix.zero());
    }

    @Override
    protected ShaTwoFiftySix computeHash() {
        try {
            return ShaTwoFiftySix.hashOf(ByteUtil.asByteArray(this::serialize));
        } catch (IOException e) {
            LOGGER.severe(e.getMessage());
            throw new RuntimeException(e);
        }
    }


    /**
     * Update the `nonce` of `this` to make the SHA-256 hash have the correct number of zeros.
     *
     * @return Whether we finished finding a valid nonce
     */
    public boolean findValidNonce() throws IOException {
        return findValidNonce(new AtomicBoolean(false));
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
        int hashGoal = Config.hashGoal();

        while (!quit.get()) {
            SHA256Digest copy = new SHA256Digest(digest);
            nonceAddOne();
            copy.update(nonce, 0, nonce.length);
            copy.doFinal(hash, 0);
            Optional<ShaTwoFiftySix> optSha = ShaTwoFiftySix.create(hash);
            if (!optSha.isPresent()) {
                LOGGER.severe("Invalid hash: %s", Arrays.toString(hash));
                break;
            } else if (optSha.get().checkHashZeros(hashGoal)) {
                return true;
            }
        }
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

    /**
     * Verifies the validity of {@code this} with respect to {@code unspentTransactions}.
     * <p>
     * A {@code Block} is said to be valid with respect to a set of unspent transactions if its inputs only contain
     * outputs from that set, it contains no double spends, and every input has a valid signature.
     *
     * @param unspentTransactions A list of unspent {@code Transaction}s that may be spent by {@code this Block}. This
     *                            collection will not be modified.
     * @return The new {@code Map} with spent transactions removed, if verification passes. Otherwise {@code Optional.empty()}.
     * @throws IOException
     */
    public Optional<UnspentTransactions> verifyNonGenesis(UnspentTransactions unspentTransactions)
            throws IOException {
        if (!this.checkHash()) {
            return Optional.empty();
        } else if (this.reward.value != REWARD_AMOUNT) {
            return Optional.empty();
        } else if (this.transactions.length != NUM_TRANSACTIONS_PER_BLOCK) {
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

    public boolean verifyGenesis(ECDSAPublicKey privilegedKey) {
        if (this.transactions.length > 0) {
            return false;
        } else if (this.reward.value != REWARD_AMOUNT) {
            return false;
        } else if (!this.checkHash()) {
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

    private static final class BlockDeserializer implements Deserializer<Block> {

        @Override
        public Block deserialize(DataInputStream input) throws DeserializationException, IOException {
            ShaTwoFiftySix hash = ShaTwoFiftySix.deserialize(input);

            Transaction[] transactions = Deserializer.deserializeList(input, Transaction.DESERIALIZER)
                    .stream().toArray(Transaction[]::new);
            if (transactions.length != 0 && transactions.length != NUM_TRANSACTIONS_PER_BLOCK) {
                String msg = String.format("Invalid number of transactions: %d", transactions.length);
                throw new DeserializationException(msg);
            }
            ECDSAPublicKey rewardKey = ECDSAPublicKey.DESERIALIZER.deserialize(input);
            Block block = new Block(hash, transactions, new TxOut(REWARD_AMOUNT, rewardKey));
            IOUtils.fill(input, block.nonce);
            return block;
        }
    }
}


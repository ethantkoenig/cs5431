package block;

import transaction.Transaction;
import transaction.TxIn;
import transaction.TxOut;
import utils.Pair;
import utils.ShaTwoFiftySix;

import java.io.*;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.*;
import java.nio.file.Paths;

/**
 * A {@code BlockChain} represents a forest of related {@code Blocks} which together represent a secure public ledger.
 * <p>
 * Created by eperdew on 2/25/17.
 */
public class BlockChain {
    private HashMap<ShaTwoFiftySix, Pair<Block, Integer>> blocks = new HashMap<>();
    private Block currentHead;
    private int headDepth;

    public BlockChain() {
        // no-op
    }

    /**
     * Creates a new {@code BlockChain} with {@code genesisBlock} as its root.
     */
    public BlockChain(Block genesisBlock) {
        blocks.put(genesisBlock.getShaTwoFiftySix(), new Pair<>(genesisBlock, 0));
        currentHead = genesisBlock;
        headDepth = 0;
    }

    /**
     * @param hash The SHA-256 hash of the block
     * @return The block corresponding to {@code hash} if it exists, or {@code Optional.empty} otherwise
     */
    public Optional<Block> getBlockWithHash(ShaTwoFiftySix hash) {
        return Optional.ofNullable(blocks.get(hash))
                .map(p -> p.getLeft());
    }

    /**
     * Inserts {@code Block b} into this {@code BlockChain}, if possible.
     * <p>
     * Fails if {@code Block b}'s parent is not in {@code this}.
     *
     * @param b The {@code Block} to insert
     * @return Whether the insertion was successful.
     */
    public boolean insertBlock(Block b) {
        if (blocks.containsKey(b.previousBlockHash)) {
            Integer depth = blocks.get(b.previousBlockHash).getRight() + 1;
            blocks.put(b.getShaTwoFiftySix(), new Pair<>(b, depth));
            if (depth > headDepth) {
                currentHead = b;
                headDepth = depth;
            }
            return true;
        } else if (b.previousBlockHash.equals(ShaTwoFiftySix.zero())) { // genesis block
            if (!blocks.isEmpty()) {
                throw new IllegalStateException("Cannot insert genesis block into non-empty blockchain");
            }
            blocks.put(b.getShaTwoFiftySix(), new Pair<>(b, 0));
            currentHead = b;
            headDepth = 0;
            return true;
        }
        return false;
    }

    /**
     * Returns the current head {@code Block} of {@code this}. The head {@code Block} is the most recent node in the
     * longest chain of {@code Block}s.
     *
     * @return The current head {@code Block} of {@code this}.
     */
    public Block getCurrentHead() {
        return currentHead;
    }

    /**
     * Finds all the ancestors of {@code Block} with hash {@code hash} contained in {@code this} and returns them from
     * youngest to oldest.
     *
     * @param hash The SHA-256 hash of the first {@code Block} that will appear in the list
     * @return A {@code List} of all ancestor {@code Block}s related to the {@code Block} with hash {@code hash}, from
     * youngest to oldest, or an empty list if no such {@code Block} exists
     */
    public List<Block> getAncestorsStartingAt(ShaTwoFiftySix hash) {
        ArrayList<Block> result = new ArrayList<>();

        if (hash == null) return result;

        while (blocks.containsKey(hash)) {
            Block current = blocks.get(hash).getLeft();
            result.add(current);
            hash = current.previousBlockHash;
        }

        return result;
    }

    /**
     * Gets a set of {@code UnspentTransactions} with respect to a {@code Block} in {@code this BlockChain}.
     *
     * @param block The {@code block} for which to generate unspent transactions. Must be in {@code this BlockChain}.
     * @return The set of unspent transactions with respect to {@code Block}
     */
    public UnspentTransactions getUnspentTransactionsAt(Block block) {
        List<Block> ancestors = getAncestorsStartingAt(block.getShaTwoFiftySix());

        UnspentTransactions unspentTxs = UnspentTransactions.empty();

        for (int i = ancestors.size() - 1; i >= 0; --i) {
            Block b = ancestors.get(i);
            for (Transaction tx : b) {

                for (int j = 0; j < tx.numInputs; ++j) {
                    TxIn inRef = tx.getInput(j);
                    unspentTxs.remove(inRef.previousTxn, inRef.txIdx);
                }
                for (int j = 0; j < tx.numOutputs; ++j) {

                    TxOut out = tx.getOutput(j);
                    unspentTxs.put(tx.getShaTwoFiftySix(), j, out);
                }
            }
            unspentTxs.put(b.getShaTwoFiftySix(), 0, b.reward);
        }

        return unspentTxs;
    }

    /**
     * Verifies a {@code Block} w.r.t. {@code this BlockChain}.
     *
     * @param block The {@code Block} to verify. It may or may not be in {@code this BlockChain}
     * @return The {@code UnspentTransactions} of {@code block}, or {@code Optional.empty()} if verification failed
     */
    public Optional<UnspentTransactions> verifyBlock(Block block) throws GeneralSecurityException, IOException {
        Optional<Block> optParent = getBlockWithHash(block.previousBlockHash);

        if (optParent.isPresent()) {
            Block parent = optParent.get();
            return block.verify(getUnspentTransactionsAt(parent));
        } else if (block.isGenesisBlock()) {
            if (block.verifyGenesis(block.reward.ownerPubKey)) {
                UnspentTransactions unspentTxs = UnspentTransactions.empty();
                unspentTxs.put(block.getShaTwoFiftySix(), 0, block.reward);
                return Optional.of(unspentTxs);
            }
        }

        return Optional.empty();
    }

    /**
     * @param hash The SHA-256 hash of the {@code Block}
     * @return Whether there is a {@code Block} with hash {@code hash} in this {@code BlockChain}
     */
    public boolean containsBlockWithHash(ShaTwoFiftySix hash) {
        return blocks.containsKey(hash);
    }

    /**
     * @param b The {@code block} to check
     * @return Whether {@code Block b} is contained within this {@code BlockChain}
     */
    public boolean containsBlock(Block b) {
        return blocks.containsKey(b.getShaTwoFiftySix());
    }

    /**
     * Serializes blockchain info, not including the blocks.
     * @param out is the output stream.
     * @throws IOException
     */
    public void serialize(DataOutputStream out) throws IOException {
        currentHead.serialize(out);
        out.writeInt(headDepth);
    }

    /**
     * Deserializes blockchain info, not including the blocks.
     * @param in is the input stream.
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public void deserialize(DataInputStream in) throws GeneralSecurityException, IOException {
        currentHead = Block.deserialize(in);
        headDepth = in.readInt();
    }

    /**
     * Writes all blocks in the main chain to the specified file. They are written in reverse
     * order, newest to oldest.
     *
     * @return true in success, exception otherwise.
     * @throws IOException
     */
    public boolean storeMainChain() throws IOException {
        Block end = currentHead;
        File blockdir = new File("blockchain");
        if (blockdir.mkdir()) {
            while (currentHead != null) {
                File block = new File("blockchain/" + currentHead.getShaTwoFiftySix());
                DataOutputStream data = new DataOutputStream(new FileOutputStream(block));
                currentHead.serialize(data);
                ShaTwoFiftySix previous = currentHead.previousBlockHash;
                if (previous.equals(ShaTwoFiftySix.zero())) break;
                currentHead = blocks.get(previous).getLeft();
            }
            currentHead = end;
            return true;
        }
        return false;
    }

    /**
     * Enumerates over all the blocks, writing the serialized blocks to a file.
     * @return true in success, exception otherwise.
     * @throws IOException
     */
    public boolean storeAllBlocks() throws IOException {
        File blockdir = new File("blockchain");
        if (blockdir.mkdir()) {
            for (Map.Entry<ShaTwoFiftySix, Pair<Block, Integer>> entry : blocks.entrySet()) {
                File block = new File("blockchain/" + entry.getKey());
                entry.getValue().getLeft().serialize(new DataOutputStream(new FileOutputStream(block)));
            }
            return true;
        }
        return false;
    }

    /**
     * Reads in a file containing a series of blocks to be used to construct the blockchain.
     * Note that since the main chain is stored in reverse order, we read in the file, deserializing
     * blocks into an arraylist, then reversing that list and reinserting the blocks into the chain.
     *
     * @return true in success, exception otherwise.
     * @throws IOException
     */
    public boolean importMainChain() throws IOException, GeneralSecurityException {
        ArrayList<Block> blocklist = new ArrayList<>();
        File blockdir = new File("blockchain");
        File[] blocks = blockdir.listFiles();
        if (blocks == null) return false;
        for (File block : blocks) {
            blocklist.add(Block.deserialize(new DataInputStream(new FileInputStream(block))));
            Files.delete(Paths.get(block.toURI()));
        }
        Collections.reverse(blocklist);
        for (Block b : blocklist) {
            insertBlock(b);
        }
        Files.delete(Paths.get("blockchain"));
        return true;
    }

    // XXX: We need to serialize depth with the block in order to store all blocks. This wll not work as is.
    private void forceInsertBlock(Block b) {
        blocks.put(b.getShaTwoFiftySix(), new Pair<>(b, -1));
    }

    /**
     * Imports a series of blocks from the specified file and reconstructs the blockchain.
     * @param file is the blockchain file.
     * @return true in success, exception otherwise.
     * @throws IOException
     */
    public boolean importAllBlocks(FileInputStream file) throws GeneralSecurityException, IOException {
        deserialize(new DataInputStream(file));
        File blockdir = new File("blockchain");
        File[] blocks = blockdir.listFiles();
        if (blocks == null) return false;
        for (File block : blocks) {
            forceInsertBlock(Block.deserialize(new DataInputStream(new FileInputStream(block))));
            Files.delete(Paths.get(block.toURI()));
        }
        return false;
    }

}

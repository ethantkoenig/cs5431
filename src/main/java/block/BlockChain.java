package block;

import transaction.RTransaction;
import transaction.RTxIn;
import transaction.RTxOut;
import utils.Pair;
import utils.ShaTwoFiftySix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

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
        blocks.put(genesisBlock.getShaTwoFiftySix(), new Pair<>(genesisBlock,0));
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

    /** Returns the current head {@code Block} of {@code this}. The head {@code Block} is the most recent node in the
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
     * @param block The {@code block} for which to generate unspent transactions
     * @return The set of unspent transactions with respect to {@code Block}
     */
    public UnspentTransactions getUnspentTransactionsAt(Block block) {
        List<Block> ancestors = getAncestorsStartingAt(block.getShaTwoFiftySix());

        UnspentTransactions unspentTxs = UnspentTransactions.empty();

        for (int i = ancestors.size() - 1; i >= 0; --i) {
            for (RTransaction tx: ancestors.get(i)) {
                for (int i = 0; i < tx.numInputs; ++i) {
                    unspentTxs.remove(tx.getShaTwoFiftySix(),i);
                }
                for (int i = 0; i < tx.numOutputs; ++i) {
                    RTxOut out = tx.getOutput(i);
                    unspentTxs.put(tx.getShaTwoFiftySix(),i,out);
                }
            }
        }

        return unspentTxs;
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

}

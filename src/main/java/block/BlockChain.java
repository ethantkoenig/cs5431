package block;

import java.util.HashMap;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

import utils.ShaTwoFiftySix;

/**
 * A {@code BlockChain} represents a forest of related {@code Blocks} which together represent a secure public ledger.
 *
 * Created by eperdew on 2/25/17.
 */
public class BlockChain {
    private HashMap<ShaTwoFiftySix, Block> blocks;

    /**
     * Creates a new empty {@code BlockChain}
     */
    public BlockChain() {
        blocks = new HashMap<>();
    }

    /**
     * @param hash The SHA-256 hash of the block
     * @return The block corresponding to {@code hash} if it exists, or {@code Optional.empty} otherwise
     */
    public Optional<Block> getBlockWithHash(ShaTwoFiftySix hash) {
        return Optional.ofNullable(blocks.get(hash));
    }

    /**
     * Inserts {@code Block b} into this {@BlockChain}
     *
     * @param b The {@code Block} to insert
     */
    public void insertBlock(Block b) {
        blocks.put(b.getShaTwoFiftySix(), b);
    }

    /**
     * Finds all the ancestors of {@code b} contained in {@code this} and returns them from youngest to oldest.
     *
     * An ancestor of a {@code Block b} is considered to be either a parent of {@code b} or a parent of an ancestor.
     * Therefore, a node is not considered to be its own ancestor.
     *
     * @param b The latest block to appear in the Block List
     * @return A {@code List} of all ancestor {@code Block}s related to {@code b}, from youngest to oldest
     */
    public List<Block> getAncestorsOf(Block b) {
        ArrayList<Block> result = new ArrayList<>();

        if (b == null) return result;

        Block current = b;
        while (blocks.containsKey(current.previousBlockHash)) {
            current = blocks.get(b);
            result.add(current);
        }

        return result;
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

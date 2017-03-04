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
     * Creates a new {@code BlockChain} with {@code genesisBlock} as its root.
     */
    public BlockChain(Block genesisBlock) {
        blocks = new HashMap<>();
        blocks.put(genesisBlock.getShaTwoFiftySix(), genesisBlock);
    }

    /**
     * @param hash The SHA-256 hash of the block
     * @return The block corresponding to {@code hash} if it exists, or {@code Optional.empty} otherwise
     */
    public Optional<Block> getBlockWithHash(ShaTwoFiftySix hash) {
        return Optional.ofNullable(blocks.get(hash));
    }

    /**
     * Inserts {@code Block b} into this {@code BlockChain}, if possible.
     *
     * Fails if {@code Block b}'s parent is not in {@code this}.
     *
     * @param b The {@code Block} to insert
     * @return Whether the insertion was successful.
     */
    public boolean insertBlock(Block b) {
        if (blocks.containsKey(b.previousBlockHash)) {
            blocks.put(b.getShaTwoFiftySix(), b);
            return true;
        }
        return false;
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
            Block current = blocks.get(hash);
            result.add(current);
            hash = current.previousBlockHash;
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

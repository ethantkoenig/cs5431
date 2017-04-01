package network;

import block.MiningBlock;
import utils.ShaTwoFiftySix;

import java.util.*;

/**
 * Data structure for tracking "orphaned" blocks
 * <p>
 * "Orphaned" blocks are blocks that cannot be added to the blockchain because
 * we don't know their full ancestry.
 */
public class OrphanedBlocks {
    private final Map<ShaTwoFiftySix, Set<MiningBlock>> blocksByPrev = new HashMap<>();

    public final void add(MiningBlock b) {
        if (!blocksByPrev.containsKey(b.previousBlockHash)) {
            blocksByPrev.put(b.previousBlockHash, new HashSet<>());
        }
        blocksByPrev.get(b.previousBlockHash).add(b);
    }

    public final List<MiningBlock> popDescendantsOf(ShaTwoFiftySix hash) {
        List<MiningBlock> result = new ArrayList<>();
        Deque<ShaTwoFiftySix> hashesToSearch = new ArrayDeque<>();
        hashesToSearch.add(hash);
        while (!hashesToSearch.isEmpty()) {
            ShaTwoFiftySix hashToSearch = hashesToSearch.poll();
            Set<MiningBlock> children = lookupAndRemove(hashToSearch);
            result.addAll(children);
            for (MiningBlock child : children) {
                hashesToSearch.add(child.getShaTwoFiftySix());
            }
        }
        return result;
    }

    private Set<MiningBlock> lookupAndRemove(ShaTwoFiftySix hash) {
        if (!blocksByPrev.containsKey(hash)) {
            return new HashSet<>();
        }
        return blocksByPrev.remove(hash);
    }

}

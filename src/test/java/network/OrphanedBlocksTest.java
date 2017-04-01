package network;

import block.MiningBlock;
import org.junit.Test;
import testutils.RandomizedTest;
import utils.ShaTwoFiftySix;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OrphanedBlocksTest extends RandomizedTest {
    @Test
    public void test1() throws Exception {
        int numBlocks = random.nextInt(100);
        List<MiningBlock> blocks = new ArrayList<>();

        MiningBlock genesis = randomBlock(ShaTwoFiftySix.zero());
        blocks.add(genesis);
        for (int i = 1; i < numBlocks; i++) {
            int parentIndex = random.nextInt(i);
            MiningBlock block = randomBlock(blocks.get(parentIndex).getShaTwoFiftySix());
            blocks.add(block);
        }

        OrphanedBlocks orphanedBlocks = new OrphanedBlocks();
        for (int i : randomPermutation(numBlocks)) {
            orphanedBlocks.add(blocks.get(i));
        }

        List<MiningBlock> poppedBlocks = orphanedBlocks.popDescendantsOf(ShaTwoFiftySix.zero());
        assertEquals(errorMessage, numBlocks, poppedBlocks.size());

        Set<MiningBlock> generatedBlocks = new HashSet<>(blocks);
        Set<ShaTwoFiftySix> encounteredBlocks = new HashSet<>();
        for (MiningBlock popped : poppedBlocks) {
            assertTrue(generatedBlocks.contains(popped));
            if (!popped.isGenesisBlock()) {
                assertTrue(encounteredBlocks.contains(popped.previousBlockHash));
            }
            encounteredBlocks.add(popped.getShaTwoFiftySix());
        }
    }
}

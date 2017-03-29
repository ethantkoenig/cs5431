package block;

import transaction.Transaction;
import transaction.TxIn;
import transaction.TxOut;
import utils.ByteUtil;
import utils.CanBeSerialized;
import utils.DeserializationException;
import utils.ShaTwoFiftySix;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

/**
 * A {@code BlockChain} represents a forest of related {@code Blocks} which together represent a secure public ledger.
 * <p>
 * Created by eperdew on 2/25/17.
 */
public class BlockChain {
    private final static Logger LOGGER = Logger.getLogger(BlockChain.class.getName());
    private LinkedHashMap<ShaTwoFiftySix, BlockWrapper> blocks = new LinkedHashMap<>();

    private Block currentHead;
    private int headDepth;
    public final Path blockStorePath;

    public BlockChain(Path blockStorePath) {
        this.blockStorePath = blockStorePath;
        try {
            Files.createDirectories(blockStorePath);
            for (Path blockPath : Files.newDirectoryStream(blockStorePath)) {
                Path filenamePath = blockPath.getFileName();
                if (filenamePath == null) {
                    continue;
                }
                String filename = filenamePath.toString();
                ShaTwoFiftySix hash = ByteUtil.hexStringToByteArray(filename)
                        .flatMap(ShaTwoFiftySix::create)
                        .orElseThrow(() -> new DeserializationException("Invalid block filename: " + filename));
                try (InputStream inputStream = new FileInputStream(blockPath.toFile())) {
                    BlockWrapper wrapper = BlockWrapper.deserialize(new DataInputStream(inputStream));
                    if (!wrapper.block.checkHash()) {
                        LOGGER.severe("Loaded block has invalid hash: " + filename);
                        continue;
                    }
                    blocks.put(hash, wrapper);
                    updateHead(wrapper);
                }
            }
        } catch (DeserializationException | IOException e) {
            LOGGER.severe("Unable to create blockchain directory: " + e.getMessage());
        }
    }
    /**
     * Creates a new {@code BlockChain} with {@code genesisBlock} as its root.
     */
    public BlockChain(Path blockStorePath, Block genesisBlock) {
        this(blockStorePath);
        if (!insertBlock(genesisBlock)) {
            LOGGER.severe("Unable to add genesis block");
        }
    }

    private void updateHead(BlockWrapper wrapper) {
        if (wrapper.depth > headDepth || currentHead == null) {
            currentHead = wrapper.block;
            headDepth = wrapper.depth;
        }
    }

    /**
     * @param hash The SHA-256 hash of the block
     * @return The block corresponding to {@code hash} if it exists, or {@code Optional.empty} otherwise
     */
    public Optional<Block> getBlockWithHash(ShaTwoFiftySix hash) {
        return getBlockWrapperWithHash(hash).map(wrapper -> wrapper.block);
    }

    private Optional<BlockWrapper> getBlockWrapperWithHash(ShaTwoFiftySix hash) {
        return Optional.ofNullable(blocks.get(hash));
    }

    private Path pathForHash(ShaTwoFiftySix hash) {
        return Paths.get(blockStorePath.toString(), hash.toString());
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
        try {
            Optional<BlockWrapper> optPrevBlock = getBlockWrapperWithHash(b.previousBlockHash);
            if (optPrevBlock.isPresent()) {
                BlockWrapper prevBlock = optPrevBlock.get();
                BlockWrapper newBlock = new BlockWrapper(b, prevBlock.depth + 1, blocks.size());
                blocks.put(b.getShaTwoFiftySix(), newBlock);
                newBlock.writeToDisk(pathForHash(b.getShaTwoFiftySix()).toFile());
                updateHead(newBlock);
                return true;
            } else if (b.previousBlockHash.equals(ShaTwoFiftySix.zero())) { // genesis block
                if (!blocks.isEmpty()) {
                    return false;
                }
                BlockWrapper newblock = new BlockWrapper(b, 0, blocks.size());
                blocks.put(b.getShaTwoFiftySix(), newblock);
                newblock.writeToDisk(pathForHash(b.getShaTwoFiftySix()).toFile());
                currentHead = b;
                headDepth = 0;
                return true;
            }
        }
        catch (IOException e) {
            LOGGER.severe(e.getMessage());
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
     * @param hash      The SHA-256 hash of the first {@code Block} that will appear in the list
     * @param numAncest the number of ancestors from {@code Block} with
     *                  {@code hash} to return.
     * @return A {@code List} of all ancestor {@code Block}s related to the
     * {@code Block} with hash {@code hash}, from youngest to oldest, or an
     * empty list if no such {@code Block} exists
     */
    public List<Block> getAncestorsStartingAt(ShaTwoFiftySix hash, int numAncest) {
        ArrayList<Block> result = new ArrayList<>();

        if (hash == null) return result;

        while (containsBlockWithHash(hash) && numAncest > 0) {
            Optional<Block> optCurrent = getBlockWithHash(hash);
            if (!optCurrent.isPresent()) {
                return result;
            }
            Block current = optCurrent.get();
            result.add(current);
            hash = current.previousBlockHash;
            --numAncest;
        }
        return result;
    }

    /**
     * Finds all the ancestors of {@code Block} with hash {@code hash} contained in {@code this} and returns them from
     * youngest to oldest.
     *
     * @param hash The SHA-256 hash of the first {@code Block} that will appear in the list
     * @return A {@code List} of all ancestor {@code Block}s related to the
     * {@code Block} with hash {@code hash}, from youngest to oldest, or an
     * empty list if no such {@code Block} exists
     */
    public List<Block> getAncestorsStartingAt(ShaTwoFiftySix hash) {
        return getAncestorsStartingAt(hash, Integer.MAX_VALUE);
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
    public Optional<UnspentTransactions> verifyBlock(Block block) throws  IOException {
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
        return getBlockWithHash(hash).isPresent();
    }

    /**
     * @param b The {@code block} to check
     * @return Whether {@code Block b} is contained within this {@code BlockChain}
     */
    public boolean containsBlock(Block b) {
        return containsBlockWithHash(b.getShaTwoFiftySix());
    }

    // TODO storing insertion position may no longer be necessary
    private static final class BlockWrapper implements Comparable<BlockWrapper>, CanBeSerialized {
        private final Block block;
        private final int depth;
        // the position in which it was inserted, used for reconstruction
        private final int insertionPosition;

        private BlockWrapper(Block block, int depth, int insertionPosition) {
            this.block = block;
            this.depth = depth;
            this.insertionPosition = insertionPosition;
        }

        public void serialize(DataOutputStream outputStream) throws IOException {
            block.serialize(outputStream);
            outputStream.writeInt(depth);
            outputStream.writeInt(insertionPosition);
        }

        private static BlockWrapper deserialize(DataInputStream input)
                throws DeserializationException, IOException {
            Block block = Block.DESERIALIZER.deserialize(input);
            int depth = input.readInt();
            int insertionPosition = input.readInt();
            return new BlockWrapper(block, depth, insertionPosition);
        }

        @Override
        public int compareTo(BlockWrapper o) {
            return insertionPosition - o.insertionPosition;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof BlockWrapper)) {
                return false;
            }
            BlockWrapper w = (BlockWrapper) o;
            return block.equals(w.block) && depth == w.depth
                    && insertionPosition == w.insertionPosition;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(new Object[]{block, depth, insertionPosition});
        }
    }
}

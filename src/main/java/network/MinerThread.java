package network;

import block.Block;

import java.util.logging.Logger;

/**
 * The MinerThread is run as a background thread by a Node and is responsible for mining.
 * This entails constantly looking for the correct nonce in order to generate a sufficiently small
 * hash value.
 *
 * @author Evan King
 * @version 1.0, Feb 22 2017
 */
public class MinerThread extends Thread {

    private static final Logger LOGGER = Logger.getLogger(MinerThread.class.getName());
    private Block block;

    public MinerThread(Block block) {
        this.block = block;
    }

    private Block tryNonces() throws Exception {
        while (true) {
            if (block.checkHash())
                return block;
            block.nonceAddOne();
        }
    }

    @Override
    public void run() {
        Block result = null;
        try {
            result = tryNonces();
        } catch (Exception e) {
            LOGGER.severe("Error hashing block: " + e.getMessage());
        }


    }


}

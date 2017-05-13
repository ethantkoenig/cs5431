package network;

import block.Block;
import crypto.ECDSAKeyPair;
import crypto.ECDSAPublicKey;
import utils.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;

public class Miner extends Node {
    private final static Log LOGGER = Log.forClass(Miner.class);

    public Miner(String name,
                 ServerSocket serverSocket,
                 ECDSAKeyPair myKeyPair,
                 ECDSAPublicKey privilegedKey,
                 Path blockChainPath) {
        super(name, serverSocket, myKeyPair, privilegedKey, blockChainPath);
    }

    public void startMiner() {
        // Start network.HandleMessageThread
        new HandleMessageThread(name, messageQueue, broadcastQueue, miningBundle, true).start();
        // Start network.BroadcastThread
        new BroadcastThread(this::broadcast, this.broadcastQueue).start();

        if (miningBundle.getKeyPair().publicKey.equals(miningBundle.privilegedKey)
                && miningBundle.getBlockChain().getCurrentHead() == null) {
            Block genesis = Block.genesis();
            genesis.addReward(miningBundle.privilegedKey);
            MinerThread minerThread = new MinerThread(name, genesis, broadcastQueue);
            minerThread.start();
        }
        // Start accepting incoming connections from other miners
        try {
            accept();
        } catch (IOException e) {
            LOGGER.severe("Error accepting incoming connections in Miner: " + e.getMessage());
        }
    }
}

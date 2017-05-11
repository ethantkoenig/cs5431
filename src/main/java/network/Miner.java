package network;

import block.Block;
import crypto.ECDSAKeyPair;
import crypto.ECDSAPublicKey;
import utils.Log;

import java.io.IOException;
import java.net.ServerSocket;

public class Miner extends Node {
    private final static Log LOGGER = Log.forClass(Miner.class);

    public Miner(ServerSocket serverSocket,
                 ECDSAKeyPair myKeyPair,
                 ECDSAPublicKey privilegedKey) {
        super(serverSocket, myKeyPair, privilegedKey);
    }

    public void startMiner() {
        // Start network.HandleMessageThread
        new HandleMessageThread(this.messageQueue, this.broadcastQueue, miningBundle, true).start();
        // Start network.BroadcastThread
        new BroadcastThread(this::broadcast, this.broadcastQueue).start();

        if (miningBundle.getKeyPair().publicKey.equals(miningBundle.privilegedKey)
                && miningBundle.getBlockChain().getCurrentHead() == null) {
            Block genesis = Block.genesis();
            genesis.addReward(miningBundle.privilegedKey);
            MinerThread minerThread = new MinerThread(genesis, broadcastQueue);
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

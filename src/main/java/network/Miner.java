package network;

import block.MiningBlock;
import block.BlockChain;
import block.UnspentTransactions;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.logging.Logger;


public class Miner extends Node {

    private final static Logger LOGGER = Logger.getLogger(Miner.class.getName());

    private MiningBundle miningBundle;

    public Miner(ServerSocket serverSocket, KeyPair myKeyPair, PublicKey privilegedKey) {
        super(serverSocket);

        Path blockChainPath = Paths.get("blockchain" + serverSocket.getLocalPort());
        BlockChain blockChain = new BlockChain(blockChainPath);
        UnspentTransactions unspentTransactions = UnspentTransactions.empty();
        miningBundle = new MiningBundle(blockChain, myKeyPair, privilegedKey, unspentTransactions);
    }

    public void connectAll(ArrayList<InetSocketAddress> hosts) {
        for (InetSocketAddress address : hosts) {
            connect(address.getHostString(), address.getPort());
        }
    }

    public void startMiner() {
        // Start network.HandleMessageThread
        new HandleMessageThread(this.messageQueue, this.broadcastQueue, miningBundle).start();
        // Start network.BroadcastThread
        new BroadcastThread(this::broadcast, this.broadcastQueue).start();

        if (miningBundle.getKeyPair().getPublic().equals(miningBundle.privilegedKey)
                && miningBundle.getBlockChain().getCurrentHead() == null) {
            MiningBlock genesis = MiningBlock.genesis();
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

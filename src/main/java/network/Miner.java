package network;

import block.Block;
import block.BlockChain;
import block.UnspentTransactions;
import crypto.ECDSAKeyPair;
import crypto.ECDSAPublicKey;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Logger;


public class Miner extends Node {

    private final static Logger LOGGER = Logger.getLogger(Miner.class.getName());

    private MiningBundle miningBundle;

    public Miner(ServerSocket serverSocket, ECDSAKeyPair myKeyPair, ECDSAPublicKey privilegedKey) {
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

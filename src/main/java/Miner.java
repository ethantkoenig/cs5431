import block.Block;
import block.BlockChain;
import block.UnspentTransactions;
import network.BroadcastThread;
import network.HandleMessageThread;
import network.MiningBundle;
import network.Node;
import utils.Crypto;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.logging.Logger;


public class Miner extends Node {

    private final static Logger LOGGER = Logger.getLogger(Miner.class.getName());

    private MiningBundle miningBundle;

    public Miner(int port) {
        super(port);
        Crypto.init();
        KeyPair keyPair = null;

        try {
            keyPair = Crypto.signatureKeyPair();
        } catch (GeneralSecurityException e) {
            LOGGER.severe(e.getMessage());
        }

        assert keyPair != null;

        Block genesis = Block.genesis();
        genesis.addReward(keyPair.getPublic());
        BlockChain blockChain = new BlockChain(genesis);
        UnspentTransactions unspentTransactions = UnspentTransactions.empty();

        miningBundle = new MiningBundle(blockChain, keyPair, unspentTransactions);

    }


    public  void connectAll(ArrayList<InetSocketAddress> hosts) {
        for (InetSocketAddress address : hosts) {
            connect(address.getHostString(), address.getPort());
        }
    }

    public void startMiner(){
        // Start network.HandleMessageThread
        new HandleMessageThread(this.messageQueue, this.broadcastQueue, miningBundle).start();
        // Start network.BroadcastThread
        new BroadcastThread(this, this.broadcastQueue).start();

        // Start accepting incoming connections from other miners
        try {
            accept();
        } catch (IOException e) {
            LOGGER.severe("Error accepting incoming connections in Miner: " + e.getMessage());
        }
    }
}

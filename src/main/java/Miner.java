import block.Block;
import block.BlockChain;
import block.UnspentTransactions;
import network.BroadcastThread;
import network.HandleMessageThread;
import network.Node;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;


public class Miner extends Node {

    private final static Logger LOGGER = Logger.getLogger(Miner.class.getName());

    private BlockChain blockChain;

    private UnspentTransactions unspentTransactions;

    public Miner() {
    }


    public  void connectAll(ArrayList<String> hosts) {
        for (String host : hosts) {
            connect(host);
        }
    }

    public  void startMiner(){

        // create genesis block and initialize blockChain
        Block genesis = Block.genesis();
        blockChain = new BlockChain(genesis);

        unspentTransactions = UnspentTransactions.empty();

        // Start network.HandleMessageThread
        new HandleMessageThread(this.messageQueue, this.broadcastQueue, blockChain, unspentTransactions).start();
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

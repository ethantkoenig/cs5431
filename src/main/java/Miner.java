import block.BlockChain;
import network.Node;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;


public class Miner extends Node {

    private final static Logger LOGGER = Logger.getLogger(Miner.class.getName());

    private BlockChain blockChain;

    public Miner() {
    }


    public  void connectAll(ArrayList<String> hosts) {
        for (String host : hosts) {
            connect(host);
        }
    }

    public  void startMiner(){

        // TODO: Start the necessary threads here. Can't do until merge in network

        // TODO: create genesis block and initialize blockChain

        try {
            accept();
        } catch (IOException e) {
            LOGGER.severe("Error accepting incoming connections in Miner.");
        }
    }
}

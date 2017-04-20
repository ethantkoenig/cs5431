package network;

import block.BlockChain;
import block.UnspentTransactions;
import crypto.ECDSAKeyPair;
import crypto.ECDSAPublicKey;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.logging.Logger;

/**
 * The network.Node class represents an arbitrary node in the network that can communicate
 * with all other nodes through the use of the broadcast function, maintains an up to date
 * blockchain, and responds to network messages.
 *
 * @version 2.0, Feb 16 2017
 */
public class Node {
    private static final Logger LOGGER = Logger.getLogger(Node.class.getName());

    private final ServerSocket serverSocket;

    // Synchronized blocking queue to hold incoming messages
    protected BlockingQueue<IncomingMessage> messageQueue;

    // Synchronized blocking queue to hold outgoing broadcast messages
    protected BlockingQueue<OutgoingMessage> broadcastQueue;

    protected MiningBundle miningBundle;

    // The connections list holds all of the Nodes current connections
    protected ArrayList<ConnectionThread> connections;

    public Node(ServerSocket serverSocket, ECDSAKeyPair myKeyPair, ECDSAPublicKey privilegedKey) {
        this.connections = new ArrayList<>();
        this.messageQueue = new SynchronousQueue<>();
        this.broadcastQueue = new SynchronousQueue<>();
        this.serverSocket = serverSocket;
        Path blockChainPath = Paths.get("blockchain" + serverSocket.getLocalPort());
        BlockChain blockChain = new BlockChain(blockChainPath);
        UnspentTransactions unspentTransactions = UnspentTransactions.empty();
        miningBundle = new MiningBundle(blockChain, myKeyPair, privilegedKey, unspentTransactions);
    }

    public void startNode() {
        // Start network.HandleMessageThread
        new HandleMessageThread(this.messageQueue, this.broadcastQueue, miningBundle, false).start();
        // Start network.BroadcastThread
        new BroadcastThread(this::broadcast, this.broadcastQueue).start();
        // Start accepting incoming connections from other miners
        try {
            accept();
        } catch (IOException e) {
            LOGGER.severe("Error accepting incoming connections in Node: " + e.getMessage());
        }
    }

    /**
     * Allows the Node to accept incoming connections from other nodes.
     * Takes each incoming connection and creates a ConnectionTread object
     * and adds it to the connections list
     *
     * @throws IOException in serverSocket.accept() if host socket has closed
     */
    public void accept() throws IOException {
        LOGGER.info("[+] Accepting connections");

        while (true) {
            ConnectionThread connectionThread = null;
            try {
                connectionThread = new ConnectionThread(serverSocket.accept(), this.messageQueue);
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
            LOGGER.info("[+] Received connection!");
            connectionThread.start();
            synchronized (this) {
                connections.add(connectionThread);
            }
        }
    }

    public void connectAll(ArrayList<InetSocketAddress> hosts) {
        for (InetSocketAddress address : hosts) {
            connect(address.getHostString(), address.getPort());
        }
    }

    /**
     * Allows this Node to connect to any other node on the network upon establishment.
     *
     * @param host is the ip address of the remote Node you wish to connect to.
     */
    public void connect(String host, int port) {
        LOGGER.info(String.format("[+] Connecting to host: %s.%n", host));
        try {
            Socket socket = new Socket(host, port);
            ConnectionThread connectionThread = new ConnectionThread(socket, this.messageQueue);
            connectionThread.start();
            synchronized (this) {
                this.connections.add(connectionThread);
            }
        } catch (IOException e) {
            LOGGER.severe(String.format("Could not connect to host: %s.%n", host));
        }
    }

    /**
     * Sends a message to all other nodes in the network that you are connected to.
     *
     * @param message the type message object containing type and payload
     */
    public synchronized void broadcast(OutgoingMessage message) {
        // broadcast message to self
        try {
            messageQueue.put(new IncomingMessage(message.type, message.payload));
        } catch (InterruptedException e) {
            LOGGER.severe(e.getMessage());
        }

        // broadcast message to others
        Set<ConnectionThread> toRemove = new HashSet<>();
        for (ConnectionThread connectionThread : connections) {
            try {
                connectionThread.send(message);
            } catch (IOException e) {
                LOGGER.warning(String.format(
                        "Lost connection to connectionThread: %s.%n", connectionThread));
                toRemove.add(connectionThread);
            }
        }
        connections.removeAll(toRemove);
    }

    /**
     * Closes the server socket
     *
     * @throws IOException if the serverSocket is already closed or broken.
     */
    public synchronized void close() throws IOException {
        if (serverSocket != null) serverSocket.close();
        for (ConnectionThread connectionThread : connections) {
            connectionThread.close();
        }
    }

}

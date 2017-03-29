package network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.logging.Logger;

/**
 * The network.Node class represents an arbitrary node in the network that can communicate
 * with all other nodes through the use of the broadcast function.
 *
 * @version 1.0, Feb 16 2017
 */
public class Node {
    private static final Logger LOGGER = Logger.getLogger(Node.class.getName());

    private final ServerSocket serverSocket;

    // Synchronized blocking queue to hold incoming messages
    protected BlockingQueue<IncomingMessage> messageQueue;

    // Synchronized blocking queue to hold outgoing broadcast messages
    protected BlockingQueue<OutgoingMessage> broadcastQueue;


    // The connections list holds all of the Nodes current connections
    protected ArrayList<ConnectionThread> connections;

    public Node(ServerSocket serverSocket) {
        this.connections = new ArrayList<>();
        this.messageQueue = new SynchronousQueue<>();
        this.broadcastQueue = new SynchronousQueue<>();
        this.serverSocket = serverSocket;
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
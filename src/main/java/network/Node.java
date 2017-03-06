package network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.logging.Logger;

/**
 * The network.Node class represents an arbitrary node in the network that can communicate
 * with all other nodes through the use of the broadcast function.
 *
 * @author Evan King
 * @version 1.0, Feb 16 2017
 * @todo error handling will need to be thoroughly tested in regards to lost connections
 */
public class Node {
    private static final Logger LOGGER = Logger.getLogger(Node.class.getName());

    private int port;
    private ServerSocket serverSocket;

    // Synchronized blocking queue to hold incoming messages
    protected BlockingQueue<Message> messageQueue;

    // Synchronized blocking queue to hold outgoing broadcast messages
    protected BlockingQueue<Message> broadcastQueue;


    // The connections list holds all of the Nodes current connections
    protected ArrayList<ConnectionThread> connections;

    public Node(int port) {
        this.connections = new ArrayList<>();
        this.messageQueue = new SynchronousQueue<>();
        this.broadcastQueue = new SynchronousQueue<>();
        this.port = port;
    }

    /**
     * Allows the Node to accept incoming connections from other nodes.
     * Takes each incoming connection and creates a ConnectionTread object
     * and adds it to the connections list
     *
     * @throws IOException in serverSocket.accept() if host socket has closed
     */
    public void accept() throws IOException {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            LOGGER.severe(String.format("Could not listen on port: %s.%n", port));
        }

        LOGGER.info("[+] Accepting connections");

        new ConnectionThread( new Socket("localhost", port) ,this.messageQueue).start();

        //Accepting connections must be done by a background thread so that we can still broadcast, etc.
        new Thread(() -> {
            while (true) {
                ConnectionThread connectionThread = null;
                try {
                    connectionThread = new ConnectionThread(serverSocket.accept(), this.messageQueue);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                LOGGER.info("[+] Received connection!");
                connectionThread.start();
                this.connections.add(connectionThread);
            }
        }
        ).start();
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
            this.connections.add(connectionThread);
        } catch (IOException e) {
            LOGGER.severe(String.format("Could not connect to host: %s.%n", host));
        }
    }

    /**
     * Sends a message to all other nodes in the network that you are connected to.
     *
     * @param message   the type message object containing type and payload
     */
    public synchronized void broadcast(Message message) {
        byte type = message.type;
        byte[] output = message.payload;
        for (ConnectionThread connectionThread : connections) {
            try {
                connectionThread.send(type, output);
            } catch (IOException e) {
                LOGGER.warning(String.format(
                        "Lost connection to connectionThread: %s.%n", connectionThread));
                this.connections.remove(connectionThread);
            }
        }
    }

    /**
     * Closes the server socket
     *
     * @throws IOException if the serverSocket is already closed or broken.
     */
    public void close() throws IOException {
        if (serverSocket != null) serverSocket.close();
        for (ConnectionThread connectionThread : connections) {
            connectionThread.close();
        }
    }

}
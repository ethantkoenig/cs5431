import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * The Node class represents an arbitrary node in the network that can communicate
 * with all other nodes through the use of the broadcast function.
 *
 * @author Evan King
 * @version 1.0, Feb 16 2017
 * @todo error handling will need to be thoroughly tested in regards to lost connections
 */
public class Node {
    private static final int PORT = 4444;
    private ServerSocket serverSocket;

    // The connections list holds all of the Nodes current connections
    protected ArrayList<ConnectionThread> connections;

    public Node() {
        this.connections = new ArrayList<>();
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
            serverSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            System.err.printf("Could not listen on port: %s.\n", PORT);
            System.exit(-1);
        }

        System.out.println("[+] Accepting connections");

        while (true) {
            ConnectionThread connectionThread = new ConnectionThread(serverSocket.accept(), this);
            connectionThread.start();
            this.connections.add(connectionThread);
        }
    }

    /**
     * Allows this Node to connect to any other node on the network upon establishment.
     *
     * @param host is the ip address of the remote Node you wish to connect to.
     */
    public void connect(String host) {
        System.out.printf("[+] Connecting to host: %s.\n", host);
        try {
            Socket socket = new Socket(host, PORT);
            ConnectionThread connectionThread = new ConnectionThread(socket, this);
            connectionThread.start();
            this.connections.add(connectionThread);
        } catch (IOException e) {
            System.err.printf("Could not connect to host: %s.\n", host);
            System.exit(-1);
        }
    }

    /**
     * Sends a message to all other nodes in the network that you are connected to.
     *
     * @param output is the message to be broadcasted.
     */
    public void broadcast(String output) {
        for (ConnectionThread connectionThread : connections) {
            try {
                connectionThread.send(output);
            } catch (IOException e) {

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

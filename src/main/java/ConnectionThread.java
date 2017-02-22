import java.net.*;
import java.io.*;

/**
 * The ConnectionThread class extends Thread and represents another node in the network that one is connected to.
 * The class allows one to send and receive messages to/from other nodes.
 *
 * @author Evan King
 * @version 1.0, Feb 16 2017
 * @todo error handling will need to be thoroughly tested in regards to lost connections
 */
public class ConnectionThread implements Runnable {
    private Node node;
    private Socket socket = null;

    // The out buffer to write to this ConnectionThread
    private PrintWriter out;

    // The in buffer to read incoming messages to this ConnectionThread
    private BufferedReader in;

    public ConnectionThread(Socket socket, Node node) {
        this.node = node;
        this.socket = socket;
        try {
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.err.printf("Unable to establish two way connection between nodes.\n");
            e.printStackTrace();
        }
    }

    /**
     * The run() function is ran when the thread is started. We initialize and start
     * a background thread to listen to incoming messages and send an initial connection message.
     */
    @Override
    public void run() {
        try {
            send("[+] Connection Established");
        } catch (IOException e) {
            System.err.printf("Unable to send to client.\n");
            e.printStackTrace();
        }

        // Start anonymous thread to handle all incoming messages in the background
        new Thread() {
            public void run() {
                System.out.println("[+] Starting to receive messages");
                receive();
            }
        }.start();
    }


    /**
     * Send the given output to this ConnectionThread
     *
     * @param output the message to be sent
     * @throws IOException if out.checkError() returns true indicating that the connection has been closed.
     */
    public void send(String output) throws IOException {
        out.println(output);

        //connection closed by remote node
        if (out.checkError())
            throw new IOException("Remote socket closed.");
    }

    /**
     * Ran by a background thread as seen in the run() function. Receives and handles all incoming messages.
     *
     * @todo Do something with received input, for now just print to stdout
     */
    public void receive() {

        String inputLine, outputLine;
        try {
            while ((inputLine = in.readLine()) != null) {
                outputLine = "We received the input: " + inputLine;
                System.out.println(outputLine);
            }
        } catch (IOException e) {
            System.err.printf("Unable to read input. Client most likely disconnected.\n");
            node.connections.remove(this);
        }
    }

    /**
     * Close connection to client socket.
     */
    public void close() {
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "ConnectionThread{" +
                "node=" + node +
                ", socket=" + socket +
                '}';
    }
}

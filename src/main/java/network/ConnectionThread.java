package network;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

/**
 * The network.ConnectionThread class extends Thread and represents another node in the network that one is connected to.
 * The class allows one to send and receive messages to/from other nodes.
 *
 * @author Evan King
 * @version 1.0, Feb 16 2017
 * @todo error handling will need to be thoroughly tested in regards to lost connections
 */
public class ConnectionThread extends Thread {
    private static final Logger LOGGER = Logger.getLogger(ConnectionThread.class.getName());

    private Socket socket = null;
    private BlockingQueue<String> queue;

    // The out buffer to write to this network.ConnectionThread
    private PrintWriter out;

    // The in buffer to read incoming messages to this network.ConnectionThread
    private BufferedReader in;

    public ConnectionThread(Socket socket, BlockingQueue<String> queue) {
        this.socket = socket;
        this.queue = queue;
        try {
            this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        } catch (IOException e) {
            LOGGER.severe("Unable to establish two way connection between nodes.%n");
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
            LOGGER.severe("Unable to send to client.%n");
            e.printStackTrace();
        }

        // Start anonymous thread to handle all incoming messages in the background
        new Thread(() -> {
            LOGGER.info("[+] Starting to receive messages");
            receive();
        }).start();
    }


    /**
     * Send the given output to this network.ConnectionThread
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

        String inputLine;
        try {
            while ((inputLine = in.readLine()) != null) {
                queue.put(inputLine);
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.severe("Unable to read input. Client most likely disconnected.%n");
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
                ", socket=" + socket +
                '}';
    }
}

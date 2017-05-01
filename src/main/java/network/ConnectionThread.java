package network;

import message.IncomingMessage;
import message.OutgoingMessage;
import utils.DeserializationException;
import utils.Deserializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

/**
 * The network.ConnectionThread class extends Thread and represents another node in the network that one is connected to.
 * The class allows one to send and receive messages to/from other nodes.
 *
 * @version 1.0, Feb 16 2017
 */
public class ConnectionThread extends Thread {
    private static final Logger LOGGER = Logger.getLogger(ConnectionThread.class.getName());

    private final Socket socket;
    private final BlockingQueue<IncomingMessage> messageQueue;

    // The out buffer to write to this network.ConnectionThread
    private DataOutputStream out;

    // The in buffer to read incoming messages to this network.ConnectionThread
    private InputStream in;

    public ConnectionThread(Socket socket, BlockingQueue<IncomingMessage> messageQueue) {
        this.socket = socket;
        this.messageQueue = messageQueue;
        try {
            this.out = new DataOutputStream(socket.getOutputStream());
            this.in = socket.getInputStream();
        } catch (IOException e) {
            LOGGER.severe("Unable to establish two way connection between nodes.%n");
            e.printStackTrace();
        }
    }

    /**
     * The run() function is run when the thread is started. We initialize and start
     * a background thread to listen to incoming messages and send an initial connection message.
     */
    @Override
    public void run() {
        try {
            receive();
        } catch (DeserializationException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
        close();
    }


    /**
     * Send the given message to this network.ConnectionThread
     *
     * @param message message to send
     * @throws IOException if out.checkError() returns true indicating that the connection has been closed.
     */
    public void send(OutgoingMessage message) throws IOException {
        message.serialize(new DataOutputStream(out));
    }

    /**
     * Ran by a background thread as seen in the run() function. Receives and handles all incoming messages.
     * Puts messages on the messageQueue to be consumed by the HandleMessageThread
     * <p>
     * Receives incoming messages, and put them onto the messageQueue.
     */
    private void receive() throws DeserializationException, IOException, InterruptedException {
        DataInputStream dataInputStream = new DataInputStream(in);
        Deserializer<IncomingMessage> deserializer = IncomingMessage.deserializer(this::send);
        while (true) {
            IncomingMessage message = deserializer.deserialize(dataInputStream);
            LOGGER.info("Putting message on messageQueue: " + message.toString());
            messageQueue.put(message);
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
        return "ConnectionThread{socket=" + socket + "}";
    }
}

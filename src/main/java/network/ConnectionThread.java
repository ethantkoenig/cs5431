package network;

import message.IncomingMessage;
import message.OutgoingMessage;
import utils.DeserializationException;
import utils.Log;

import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;

/**
 * The network.ConnectionThread class extends Thread and represents another node in the network that one is connected to.
 * The class allows one to send and receive messages to/from other nodes.
 *
 * @version 1.0, Feb 16 2017
 */
public class ConnectionThread extends Thread {
    private static final Log LOGGER = Log.forClass(ConnectionThread.class);

    private final Connection connection;
    private final BlockingQueue<IncomingMessage> messageQueue;

    public ConnectionThread(Connection connection, BlockingQueue<IncomingMessage> messageQueue) {
        this.connection = connection;
        this.messageQueue = messageQueue;
    }

    public boolean isBroadcastConnection() {
        return connection.isBroadcastConnection;
    }

    /**
     * The run() function is run when the thread is started. We initialize and start
     * a background thread to listen to incoming messages and send an initial connection message.
     */
    @Override
    public void run() {
        try {
            receive();
        } catch (EOFException e) {
            LOGGER.info("[-] Connection closed");
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
        connection.send(message);
    }

    /**
     * Ran by a background thread as seen in the run() function. Receives and handles all incoming messages.
     * Puts messages on the messageQueue to be consumed by the HandleMessageThread
     * <p>
     * Receives incoming messages, and put them onto the messageQueue.
     */
    private void receive() throws DeserializationException, IOException, InterruptedException {
        while (true) {
            messageQueue.put(connection.receive());
        }
    }

    /**
     * Close connection to client socket.
     */
    public void close() {
        try {
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

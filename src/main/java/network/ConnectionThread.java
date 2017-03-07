package network;

import utils.IOUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
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

    private static final int MAX_PAYLOAD_LEN = 33554432;

    private final Socket socket;
    private final BlockingQueue<Message> messageQueue;

    // The out buffer to write to this network.ConnectionThread
    private DataOutputStream out;

    // The in buffer to read incoming messages to this network.ConnectionThread
    private InputStream in;

    public ConnectionThread(Socket socket, BlockingQueue<Message> messageQueue) {
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
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        close();
    }


    /**
     * Send the given output to this network.ConnectionThread
     *
     * @param type   the type of message to be sent
     * @param payload the message to be sent
     * @throws IOException if out.checkError() returns true indicating that the connection has been closed.
     */
    public void send(byte type, byte[] payload) throws IOException {
        IOUtils.sendMessage(out, type, payload);
    }

    /**
     * Ran by a background thread as seen in the run() function. Receives and handles all incoming messages.
     * Puts messages on the messageQueue to be consumed by the HandleMessageThread
     *
     * Receives incoming messages, and put them onto the messageQueue.
     */
    private void receive() throws IOException, InterruptedException {
        byte[] headerBuffer = new byte[Integer.BYTES + Byte.BYTES];
        while (true) {
            try {
                IOUtils.fill(in, headerBuffer);
            }catch (IOException e){
                LOGGER.info("[-] Lost connection to Node: " + socket.getInetAddress().getHostAddress());
                close();
                break;
            }
            int payloadLen = ByteBuffer.wrap(headerBuffer, 0, Integer.BYTES).getInt();
            if (payloadLen > MAX_PAYLOAD_LEN) {
                LOGGER.severe(String.format("Received misformatted message (payloadLen=%d)", payloadLen));
            }
            byte payloadType = ByteBuffer.wrap(headerBuffer, Integer.BYTES, Byte.BYTES).get();
            byte[] payload = new byte[payloadLen];
            IOUtils.fill(in, payload);
            Message message = new Message(payloadType, payload);
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

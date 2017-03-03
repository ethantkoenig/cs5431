package network;

import utils.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    private final BlockingQueue<Message> queue;

    // The out buffer to write to this network.ConnectionThread
    private OutputStream out;

    // The in buffer to read incoming messages to this network.ConnectionThread
    private InputStream in;

    public ConnectionThread(Socket socket, BlockingQueue<Message> queue) {
        this.socket = socket;
        this.queue = queue;
        try {
            this.out = socket.getOutputStream();
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
            close();
        } catch (IOException | InterruptedException e) {
            LOGGER.severe(e.getMessage());
        }
    }


    /**
     * Send the given output to this network.ConnectionThread
     *
     * @param type   the type of message to be sent
     * @param output the message to be sent
     * @throws IOException if out.checkError() returns true indicating that the connection has been closed.
     */
    public void send(byte type, byte[] output) throws IOException {
        out.write(ByteBuffer.allocate(Integer.BYTES).putInt(output.length).array());
        out.write(type);
        out.write(output);
    }

    /**
     * Receives incoming messages, and put them onto the queue.
     */
    private void receive() throws IOException, InterruptedException {
        byte[] headerBuffer = new byte[Integer.BYTES + Byte.BYTES];
        while (true) {
            IOUtils.fill(in, headerBuffer);
            int payloadLen = ByteBuffer.wrap(headerBuffer, 0, Integer.BYTES).getInt();
            if (payloadLen > MAX_PAYLOAD_LEN) {
                LOGGER.severe(String.format("Received misformatted message (payloadLen=%d)", payloadLen));
                return;
            }
            byte payloadType = ByteBuffer.wrap(headerBuffer, Integer.BYTES, Byte.BYTES).get();
            byte[] payload = new byte[payloadLen];
            IOUtils.fill(in, payload);
            Message message = Message.create(payloadType, payload);
            queue.put(message);
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

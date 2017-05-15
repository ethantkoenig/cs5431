package network;

import message.IncomingMessage;
import message.OutgoingMessage;
import utils.DeserializationException;
import utils.Deserializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Connection implements AutoCloseable {
    private static final byte BROADCAST_CONNECTION = 0;
    private static final byte NON_BROADCAST_CONNECTION = 1;

    private final Socket socket;

    // The out buffer to write to
    private final DataOutputStream out;

    // The in buffer to read incoming messages from
    private final DataInputStream in;

    // If this connection should be forwarded broadcasts
    final boolean isBroadcastConnection;

    private Connection(Socket socket,
                      DataOutputStream out,
                      DataInputStream in,
                      boolean isBroadcastConnection) {
        this.socket = socket;
        this.out = out;
        this.in = in;
        this.isBroadcastConnection = isBroadcastConnection;
    }

    public static Connection accept(Socket socket) throws IOException {
        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
        DataInputStream inputStream = new DataInputStream(socket.getInputStream());
        byte connectionType = inputStream.readByte();
        boolean isBroadcastConnection;
        switch (connectionType) {
            case BROADCAST_CONNECTION:
                isBroadcastConnection = true;
                break;
            case NON_BROADCAST_CONNECTION:
                isBroadcastConnection = false;
                break;
            default:
                String msg = String.format("Unexpected connection type: %d", connectionType);
                throw new IOException(msg);
        }
        return new Connection(socket, outputStream, inputStream, isBroadcastConnection);
    }

    public static Connection connect(Socket socket, boolean isBroadcastConnection)
            throws IOException {
        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
        if (isBroadcastConnection) {
            outputStream.writeByte(BROADCAST_CONNECTION);
        } else {
            outputStream.writeByte(NON_BROADCAST_CONNECTION);
        }
        DataInputStream inputStream = new DataInputStream(socket.getInputStream());
        return new Connection(socket, outputStream, inputStream, isBroadcastConnection);
    }

    public IncomingMessage receive() throws DeserializationException, IOException {
        Deserializer<IncomingMessage> deserializer = IncomingMessage.deserializer(this::send);
        return deserializer.deserialize(in);
    }

    public void send(OutgoingMessage message) throws IOException {
        message.serialize(out);
    }

    /**
     * Close connection to client socket.
     */
    public void close() throws IOException {
        in.close();
        out.close();
        socket.close();
    }
}

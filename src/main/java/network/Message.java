package network;

import java.util.Arrays;

/**
 * Represents a message sent or received on the network
 */
public class Message {
    /* Message kinds */
    public static final byte TRANSACTION = 0;
    public static final byte BLOCK = 1;

    public final byte type;
    public final byte[] payload;

    public Message(byte type, byte[] payload) {
        this.type = type;
        this.payload = Arrays.copyOf(payload, payload.length);
    }
}

package message;

import java.util.Arrays;

/**
 * Represents a message sent or received on the network
 */
public abstract class Message {
    public static final int MAX_PAYLOAD_LEN = 33554432;
    public static final int MAX_BLOCKS_TO_GET = 32;

    /* Message types */
    public static final byte TRANSACTION = 0;
    public static final byte BLOCKS = 1;
    public static final byte GET_BLOCKS = 2;
    public static final byte GET_FUNDS = 3;
    public static final byte FUNDS = 4;
    public static final byte GET_UTX_WITH_KEYS = 5;
    public static final byte UTX_WITH_KEYS = 6;
    public static final byte PING = 7;
    public static final byte PONG = 8;

    public final byte type;
    public final byte[] payload;

    public Message(byte type, byte[] payload) {
        this.type = type;
        this.payload = Arrays.copyOf(payload, payload.length);
    }

    @Override
    public String toString() {
        return String.format("Message[type=%d, payload={len:%d}]", type, payload.length);
    }
}

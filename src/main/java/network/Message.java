package network;

import utils.ByteUtil;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Represents a message sent or received on the network
 */
public abstract class Message {
    public static final int MAX_PAYLOAD_LEN = 33554432;

    /* Message types */
    public static final byte TRANSACTION = 0;
    public static final byte BLOCK = 1;
    public static final byte GET_BLOCK = 2;
    public static final byte GET_HEAD = 3;

    public final byte type;
    public final byte[] payload;

    public Message(byte type, byte[] payload) {
        this.type = type;
        this.payload = Arrays.copyOf(payload, payload.length);
    }
}

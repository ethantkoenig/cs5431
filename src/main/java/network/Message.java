package network;

import utils.ByteUtil;

import java.util.Arrays;

/**
 * Represents a message sent or received on the network
 *
 * @version 1.0, Feb 28 2017
 */
public class Message {
    /* Message kinds */
    public static final byte TRANSACTION = 0;
    public static final byte BLOCK = 1;
    public static final byte getBLOCK = 2;

    public final byte type;
    public final byte[] payload;

    public Message(byte type, byte[] payload) {
        this.type = type;
        this.payload = Arrays.copyOf(payload, payload.length);
    }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", payload=" + ByteUtil.bytesToHexString(payload) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message message = (Message) o;

        if (type != message.type) return false;
        return Arrays.equals(payload, message.payload);

    }

    @Override
    public int hashCode() {
        int result = (int) type;
        result = 31 * result + Arrays.hashCode(payload);
        return result;
    }
}

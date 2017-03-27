package network;

import utils.ShaTwoFiftySix;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Represents a message sent or received on the network
 */
public abstract class Message {
    public static final int MAX_PAYLOAD_LEN = 33554432;
    public static final int MAX_BLOCKS_TO_GET = 32;

    /* Message types */
    public static final byte TRANSACTION = 0;
    public static final byte BLOCK = 1;
    public static final byte GET_BLOCK = 2;

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

    /**
     * Write the serialization of a payload of GET_BLOCK message type
     *
     * @param hash     A ShaTwofiftysix hash object of the requesting hash
     * @param numToGet int of the amount of ancestors to get of {@code hash}}
     * @return A byte array which is formatted as a payload of the GET_BLOCK
     * message format
     */
    public static byte[] getBlockPayload(ShaTwoFiftySix hash, int numToGet)
            throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(outputStream);
        hash.writeTo(dataOut);
        dataOut.writeInt(numToGet);
        return outputStream.toByteArray();
    }
}

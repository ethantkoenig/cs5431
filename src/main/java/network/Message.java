package network;

/**
 * Represents a message sent or received on the network
 */
public class Message {
    /* Message kinds */
    public static final byte TRANSACTION = 0;
    public static final byte BLOCK = 1;

    public final byte type;
    public final byte[] payload;

    private Message(byte type, byte[] payload) {
        this.type = type;
        this.payload = payload;
    }

    /**
     * Create a new message with an initially-null payload.
     *
     * @param type       the type of the message
     * @param payloadLen the length of the payload
     * @return a newly-create {@code Message}
     */
    public static Message create(byte type, int payloadLen) {
        /* allocate the payload array inside a static constructor, so that the
         * creator of the Message doesn't have another reference to the array.
         * FindBugs prefers it this way */
        byte[] payload = new byte[payloadLen];
        return new Message(type, payload);
    }
}

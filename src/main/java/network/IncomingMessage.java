package network;


import utils.IOUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Logger;

public class IncomingMessage extends Message {
    private static final Logger LOGGER = Logger.getLogger(IncomingMessage.class.getName());

    private final MessageResponder responder; // may be null

    public IncomingMessage(byte type, byte[] payload) {
        this(type, payload, null);
    }

    public IncomingMessage(byte type, byte[] payload,
                           MessageResponder responder) {
        super(type, payload);
        this.responder = responder;
    }

    public void respond(OutgoingMessage message) throws IOException {
        if (responder == null) {
            LOGGER.warning("Cannot response to this message: " + this);
            return;
        }
        responder.respond(message);
    }

    public static Optional<IncomingMessage> deserialize(DataInputStream inputStream,
                                                        MessageResponder responder)
            throws IOException {
        int payloadLen = inputStream.readInt();
        if (payloadLen < 0 || payloadLen >= MAX_PAYLOAD_LEN) {
            LOGGER.severe(String.format("Received misformatted message (payloadLen=%d)", payloadLen));
            return Optional.empty();
        }
        byte type = inputStream.readByte();
        byte[] payload = new byte[payloadLen];
        IOUtils.fill(inputStream, payload);
        return Optional.of(new IncomingMessage(type, payload, responder));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof IncomingMessage)) {
            return false;
        }
        IncomingMessage other = (IncomingMessage) o;
        return other.type == type && Arrays.equals(other.payload, payload);
    }

    @Override
    public int hashCode() {
        return type + 31 * Arrays.hashCode(payload);
    }

    @FunctionalInterface
    public interface MessageResponder {
        void respond(OutgoingMessage message) throws IOException;
    }
}

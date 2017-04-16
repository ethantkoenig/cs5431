package network;


import utils.DeserializationException;
import utils.Deserializer;
import utils.IOUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
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

    public static Deserializer<IncomingMessage> responderlessDeserializer() {
        return new IncomingMessageDeserializer(null);
    }

    public static Deserializer<IncomingMessage> deserializer(MessageResponder responder) {
        return new IncomingMessageDeserializer(responder);
    }

    public void respond(OutgoingMessage message) throws IOException {
        if (responder == null) {
            LOGGER.warning("Cannot response to this message: " + this);
            return;
        }
        responder.respond(message);
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

    private static final class IncomingMessageDeserializer implements Deserializer<IncomingMessage> {
        private final MessageResponder responder;

        private IncomingMessageDeserializer(MessageResponder responder) {
            this.responder = responder;
        }

        @Override
        public IncomingMessage deserialize(DataInputStream inputStream) throws DeserializationException, IOException {
            int payloadLen = inputStream.readInt();
            if (payloadLen < 0 || payloadLen >= MAX_PAYLOAD_LEN) {
                String msg = String.format("Received misformatted message (payloadLen=%d)", payloadLen);
                throw new DeserializationException(msg);
            }
            byte type = inputStream.readByte();
            byte[] payload = new byte[payloadLen];
            IOUtils.fill(inputStream, payload);
            return new IncomingMessage(type, payload, responder);
        }
    }
}

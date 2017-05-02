package message.payloads;

import message.OutgoingMessage;
import utils.ByteUtil;
import utils.CanBeSerialized;

import java.io.IOException;

public abstract class MessagePayload implements CanBeSerialized {

    public abstract byte messageType();

    public OutgoingMessage toMessage() throws IOException {
        byte[] payload = ByteUtil.asByteArray(this::serialize);
        return new OutgoingMessage(messageType(), payload);
    }
}

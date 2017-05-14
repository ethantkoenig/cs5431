package message.payloads;

import message.Message;

import java.io.DataOutputStream;
import java.io.IOException;

public class BadRequestPayload extends MessagePayload {
    @Override
    public byte messageType() {
        return Message.BAD_REQUEST;
    }

    @Override
    public void serialize(DataOutputStream outputStream) throws IOException {
    }
}

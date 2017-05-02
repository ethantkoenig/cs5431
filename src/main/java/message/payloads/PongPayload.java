package message.payloads;

import message.Message;
import utils.DeserializationException;
import utils.Deserializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PongPayload extends MessagePayload {
    public static final Deserializer<PongPayload> DESERIALIZER = new PongDeserializer();
    public final int pingNumber;

    public PongPayload(int pingNumber) {
        this.pingNumber = pingNumber;
    }

    @Override
    public void serialize(DataOutputStream outputStream) throws IOException {
        outputStream.writeInt(pingNumber);
    }

    @Override
    public byte messageType() {
        return Message.PONG;
    }

    private static class PongDeserializer implements Deserializer<PongPayload> {
        @Override
        public PongPayload deserialize(DataInputStream inputStream) throws DeserializationException, IOException {
            return new PongPayload(inputStream.readInt());
        }
    }
}

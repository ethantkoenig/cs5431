package message.payloads;

import message.Message;
import utils.DeserializationException;
import utils.Deserializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PingPayload extends MessagePayload {
    public static final Deserializer<PingPayload> DESERIALIZER = new PingDeserializer();
    public final int pingNumber;

    public PingPayload(int pingNumber) {
        this.pingNumber = pingNumber;
    }

    @Override
    public void serialize(DataOutputStream outputStream) throws IOException {
        outputStream.writeInt(pingNumber);
    }

    @Override
    public byte messageType() {
        return Message.PING;
    }

    private static class PingDeserializer implements Deserializer<PingPayload> {
        @Override
        public PingPayload deserialize(DataInputStream inputStream) throws DeserializationException, IOException {
            return new PingPayload(inputStream.readInt());
        }
    }
}

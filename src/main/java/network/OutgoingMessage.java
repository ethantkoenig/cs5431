package network;


import java.io.DataOutputStream;
import java.io.IOException;

public class OutgoingMessage extends Message {
    public OutgoingMessage(byte type, byte[] payload) {
        super(type, payload);
    }

    public void serialize(DataOutputStream outputStream) throws IOException {
        outputStream.writeInt(payload.length);
        outputStream.writeByte(type);
        outputStream.write(payload);
    }
}

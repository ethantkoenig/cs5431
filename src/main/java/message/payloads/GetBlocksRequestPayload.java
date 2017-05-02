package message.payloads;

import message.Message;
import utils.DeserializationException;
import utils.Deserializer;
import utils.ShaTwoFiftySix;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class GetBlocksRequestPayload extends MessagePayload {
    public final static Deserializer<GetBlocksRequestPayload> DESERIALIZER =
            new GetBlocksRequestDeserializer();

    public final ShaTwoFiftySix hash;
    public final int numBlocksRequested;

    public GetBlocksRequestPayload(ShaTwoFiftySix hash, int numBlocksRequested) {
        this.hash = hash;
        this.numBlocksRequested = numBlocksRequested;
    }

    @Override
    public void serialize(DataOutputStream outputStream) throws IOException {
        hash.writeTo(outputStream);
        outputStream.writeInt(numBlocksRequested);
    }

    @Override
    public byte messageType() {
        return Message.GET_BLOCKS;
    }

    private static final class GetBlocksRequestDeserializer
            implements Deserializer<GetBlocksRequestPayload> {

        @Override
        public GetBlocksRequestPayload deserialize(DataInputStream inputStream) throws DeserializationException, IOException {
            ShaTwoFiftySix hash = ShaTwoFiftySix.deserialize(inputStream);
            int numBlocksRequested = inputStream.readInt();
            return new GetBlocksRequestPayload(hash, numBlocksRequested);
        }
    }
}

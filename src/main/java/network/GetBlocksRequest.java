package network;

import utils.CanBeSerialized;
import utils.DeserializationException;
import utils.Deserializer;
import utils.ShaTwoFiftySix;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class GetBlocksRequest implements CanBeSerialized {
    public final static Deserializer<GetBlocksRequest> DESERIALIZER =
            new GetBlocksRequestDeserializer();

    public final ShaTwoFiftySix hash;
    public final int numBlocksRequested;

    public GetBlocksRequest(ShaTwoFiftySix hash, int numBlocksRequested) {
        this.hash = hash;
        this.numBlocksRequested = numBlocksRequested;
    }

    @Override
    public void serialize(DataOutputStream outputStream) throws IOException {
        hash.writeTo(outputStream);
        outputStream.writeInt(numBlocksRequested);
    }

    private static final class GetBlocksRequestDeserializer
            implements Deserializer<GetBlocksRequest> {

        @Override
        public GetBlocksRequest deserialize(DataInputStream inputStream) throws DeserializationException, IOException {
            ShaTwoFiftySix hash = ShaTwoFiftySix.deserialize(inputStream);
            int numBlocksRequested = inputStream.readInt();
            return new GetBlocksRequest(hash, numBlocksRequested);
        }
    }
}

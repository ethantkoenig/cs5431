package network;

import crypto.ECDSAPublicKey;
import utils.CanBeSerialized;
import utils.DeserializationException;
import utils.Deserializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GetFundsRequest implements CanBeSerialized {

    public final static Deserializer<GetFundsRequest> DESERIALIZER =
        new GetFundsRequestDeserializer();

    public final int numKeys;
    public final List<ECDSAPublicKey> requestedKeys;

    public GetFundsRequest(int numKeys, List<ECDSAPublicKey> requestedKeys) {
        this.numKeys = numKeys;
        this.requestedKeys = requestedKeys;
    }

    @Override
    public void serialize(DataOutputStream outputStream) throws IOException {
        outputStream.writeInt(numKeys);
        for (ECDSAPublicKey key : requestedKeys) {
            key.serialize(outputStream);
        }
    }

    private static final class GetFundsRequestDeserializer
        implements Deserializer<GetFundsRequest> {

        @Override
        public GetFundsRequest deserialize(DataInputStream inputStream)
            throws DeserializationException, IOException {
            int numKeys = inputStream.readInt();
            ArrayList<ECDSAPublicKey> reqKeys = new ArrayList<ECDSAPublicKey>();
            for (int i = 0; i < numKeys; ++i) {
                reqKeys.add(ECDSAPublicKey.DESERIALIZER.deserialize(inputStream));
            }
            return new GetFundsRequest(numKeys, reqKeys);
        }
    }
}

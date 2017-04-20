package network;

import crypto.ECDSAPublicKey;
import utils.CanBeSerialized;
import utils.DeserializationException;
import utils.Deserializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GetFundsResponse implements CanBeSerialized {
    public final static Deserializer<GetFundsResponse> DESERIALIZER =
            new GetFundsResponseDeserializer();

    public final Map<ECDSAPublicKey, Long> keyFunds;

    public GetFundsResponse(Map<ECDSAPublicKey, Long> keyFunds) {
        this.keyFunds = keyFunds;
    }

    @Override
    public void serialize(DataOutputStream outputStream) throws IOException {
        outputStream.writeInt(keyFunds.size());
        for (Map.Entry<ECDSAPublicKey, Long> entry : keyFunds.entrySet()) {
            entry.getKey().serialize(outputStream);
            outputStream.writeLong(entry.getValue());
        }
    }

    private static final class GetFundsResponseDeserializer
            implements Deserializer<GetFundsResponse> {

        @Override
        public GetFundsResponse deserialize(DataInputStream inputStream)
                throws DeserializationException, IOException {
            int numKeys = inputStream.readInt();
            if (numKeys < 0 || numKeys > Deserializer.DEFAULT_MAX_LIST_LENGTH) {
                throw new DeserializationException("Invalid number of keys");
            }
            HashMap<ECDSAPublicKey, Long> keyFunds = new HashMap<ECDSAPublicKey, Long>();
            for (int i = 0; i < numKeys; ++i) {
                ECDSAPublicKey key = ECDSAPublicKey.DESERIALIZER.deserialize(inputStream);
                long money = inputStream.readLong();
                keyFunds.put(key, money);
            }
            return new GetFundsResponse(keyFunds);
        }
    }
}

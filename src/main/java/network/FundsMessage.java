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

public class FundsMessage implements CanBeSerialized {
    public final static Deserializer<FundsMessage> DESERIALIZER =
        new FundsMessageDeserializer();

    public final int numKeys;
    public final Map<ECDSAPublicKey, Long> keyFunds;

    public FundsMessage(int numKeys, Map<ECDSAPublicKey, Long> keyFunds) {
        this.numKeys = numKeys;
        this.keyFunds = keyFunds;
    }

    @Override
    public void serialize(DataOutputStream outputStream) throws IOException {
        outputStream.writeInt(numKeys);
        for (Map.Entry<ECDSAPublicKey, Long> entry : keyFunds.entrySet()) {
            entry.getKey().serialize(outputStream);
            outputStream.writeLong(entry.getValue());
        }
    }

    private static final class FundsMessageDeserializer
        implements Deserializer<FundsMessage> {

        @Override
        public FundsMessage deserialize(DataInputStream inputStream)
            throws DeserializationException, IOException {
            int numKeys = inputStream.readInt();
            HashMap<ECDSAPublicKey, Long> keyFunds = new HashMap<ECDSAPublicKey, Long>();
            for (int i = 0; i < numKeys; ++i) {
                ECDSAPublicKey key = ECDSAPublicKey.DESERIALIZER.deserialize(inputStream);
                long money = inputStream.readLong();
                keyFunds.put(key, money);
            }
            return new FundsMessage(numKeys, keyFunds);
        }
    }
}

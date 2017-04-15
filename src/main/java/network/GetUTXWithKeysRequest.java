package network;


import crypto.ECDSAPublicKey;
import utils.CanBeSerialized;
import utils.DeserializationException;
import utils.Deserializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class GetUTXWithKeysRequest implements CanBeSerialized {

    public final List<ECDSAPublicKey> keys;
    public final ECDSAPublicKey changeKey;
    public final ECDSAPublicKey destination;
    public final long amount;

    public final static Deserializer<GetUTXWithKeysRequest> DESERIALIZER =
            new GetUTXWithKeysRequestDeserializer();

    public GetUTXWithKeysRequest(List<ECDSAPublicKey> keys, ECDSAPublicKey changeKey, ECDSAPublicKey destination, long amount) {
        this.keys = keys;
        this.changeKey = changeKey;
        this.destination = destination;
        this.amount = amount;
    }

    @Override
    public void serialize(DataOutputStream outputStream) throws IOException {
        CanBeSerialized.serializeList(outputStream, keys);
        changeKey.serialize(outputStream);
        destination.serialize(outputStream);
        outputStream.writeLong(amount);
    }

    private static final class GetUTXWithKeysRequestDeserializer
            implements Deserializer<GetUTXWithKeysRequest> {

        @Override
        public GetUTXWithKeysRequest deserialize(DataInputStream inputStream)
                throws DeserializationException, IOException {
            List<ECDSAPublicKey> keys = Deserializer.deserializeList(inputStream, ECDSAPublicKey.DESERIALIZER);
            ECDSAPublicKey changeKey = ECDSAPublicKey.DESERIALIZER.deserialize(inputStream);
            ECDSAPublicKey destination = ECDSAPublicKey.DESERIALIZER.deserialize(inputStream);
            long amount = inputStream.readLong();

            return new GetUTXWithKeysRequest(keys, changeKey, destination, amount);
        }
    }
}

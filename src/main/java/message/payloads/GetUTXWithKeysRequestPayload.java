package message.payloads;


import crypto.ECDSAPublicKey;
import message.Message;
import utils.CanBeSerialized;
import utils.DeserializationException;
import utils.Deserializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class GetUTXWithKeysRequestPayload extends MessagePayload {

    public final List<ECDSAPublicKey> keys;
    public final ECDSAPublicKey changeKey;
    public final ECDSAPublicKey destination;
    public final long amount;

    public final static Deserializer<GetUTXWithKeysRequestPayload> DESERIALIZER =
            new GetUTXWithKeysRequestDeserializer();

    public GetUTXWithKeysRequestPayload(List<ECDSAPublicKey> keys, ECDSAPublicKey changeKey, ECDSAPublicKey destination, long amount) {
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

    @Override
    public byte messageType() {
        return Message.GET_UTX_WITH_KEYS;
    }

    private static final class GetUTXWithKeysRequestDeserializer
            implements Deserializer<GetUTXWithKeysRequestPayload> {

        @Override
        public GetUTXWithKeysRequestPayload deserialize(DataInputStream inputStream)
                throws DeserializationException, IOException {
            List<ECDSAPublicKey> keys = Deserializer.deserializeList(inputStream, ECDSAPublicKey.DESERIALIZER);
            ECDSAPublicKey changeKey = ECDSAPublicKey.DESERIALIZER.deserialize(inputStream);
            ECDSAPublicKey destination = ECDSAPublicKey.DESERIALIZER.deserialize(inputStream);
            long amount = inputStream.readLong();

            return new GetUTXWithKeysRequestPayload(keys, changeKey, destination, amount);
        }
    }
}

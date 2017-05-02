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

public class GetFundsRequestPayload extends MessagePayload {

    public final static Deserializer<GetFundsRequestPayload> DESERIALIZER =
            new GetFundsRequestDeserializer();

    public final List<ECDSAPublicKey> requestedKeys;

    public GetFundsRequestPayload(List<ECDSAPublicKey> requestedKeys) {
        this.requestedKeys = requestedKeys;
    }

    @Override
    public void serialize(DataOutputStream outputStream) throws IOException {
        CanBeSerialized.serializeList(outputStream, requestedKeys);
    }

    @Override
    public byte messageType() {
        return Message.GET_FUNDS;
    }

    private static final class GetFundsRequestDeserializer
            implements Deserializer<GetFundsRequestPayload> {

        @Override
        public GetFundsRequestPayload deserialize(DataInputStream inputStream)
                throws DeserializationException, IOException {
            List<ECDSAPublicKey> reqKeys = Deserializer.deserializeList(inputStream, ECDSAPublicKey.DESERIALIZER);
            return new GetFundsRequestPayload(reqKeys);
        }
    }
}

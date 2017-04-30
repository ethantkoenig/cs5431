package message.payloads;

import crypto.ECDSAPublicKey;
import message.Message;
import transaction.Transaction;
import utils.ByteUtil;
import utils.CanBeSerialized;
import utils.DeserializationException;
import utils.Deserializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class GetUTXWithKeysResponsePayload extends MessagePayload {

    private static final int MAX_PAYLOAD_LEN = 1048576;

    public final boolean wasSuccessful;
    /**
     * Will be null if unsuccessful
     */
    public final List<ECDSAPublicKey> keysUsed;
    /**
     * Will be `null` if unsuccessful
     */
    public final byte[] unsignedTransaction;

    public final static Deserializer<GetUTXWithKeysResponsePayload> DESERIALIZER =
            new GetUTXWithKeysResponseDeserializer();

    private GetUTXWithKeysResponsePayload(List<ECDSAPublicKey> keysUsed, byte[] unsignedTransaction, boolean wasSuccessful) {
        this.keysUsed = keysUsed;
        this.unsignedTransaction = unsignedTransaction;
        this.wasSuccessful = wasSuccessful;
    }

    public static GetUTXWithKeysResponsePayload success(List<ECDSAPublicKey> keysUsed, Transaction unsignedTransaction) {
        try {
            return new GetUTXWithKeysResponsePayload(
                    keysUsed,
                    ByteUtil.asByteArray(unsignedTransaction::serializeWithoutSignatures),
                    true
            );
        } catch (IOException e) {
            e.printStackTrace();
            return failure();
        }
    }

    public static GetUTXWithKeysResponsePayload failure() {
        return new GetUTXWithKeysResponsePayload(null, null, false);
    }


    @Override
    public void serialize(DataOutputStream outputStream) throws IOException {
        outputStream.writeBoolean(wasSuccessful);
        if (wasSuccessful) {
            CanBeSerialized.serializeList(outputStream, keysUsed);
            CanBeSerialized.serializeBytes(outputStream, unsignedTransaction);
        }
    }

    @Override
    public byte messageType() {
        return Message.UTX_WITH_KEYS;
    }

    private static final class GetUTXWithKeysResponseDeserializer
            implements Deserializer<GetUTXWithKeysResponsePayload> {

        @Override
        public GetUTXWithKeysResponsePayload deserialize(DataInputStream inputStream) throws DeserializationException, IOException {
            boolean wasSuccessful = inputStream.readBoolean();
            if (wasSuccessful) {
                List<ECDSAPublicKey> keysUsed = Deserializer
                        .deserializeList(inputStream, ECDSAPublicKey.DESERIALIZER);
                byte[] unsignedTransaction = Deserializer.deserializeBytes(inputStream, MAX_PAYLOAD_LEN);

                return new GetUTXWithKeysResponsePayload(keysUsed, unsignedTransaction, true);
            } else {
                return failure();
            }
        }
    }
}

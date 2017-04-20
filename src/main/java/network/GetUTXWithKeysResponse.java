package network;

import crypto.ECDSAPublicKey;
import transaction.Transaction;
import utils.ByteUtil;
import utils.CanBeSerialized;
import utils.DeserializationException;
import utils.Deserializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class GetUTXWithKeysResponse implements CanBeSerialized {

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

    public final static Deserializer<GetUTXWithKeysResponse> DESERIALIZER =
            new GetUTXWithKeysResponseDeserializer();

    private GetUTXWithKeysResponse(List<ECDSAPublicKey> keysUsed, byte[] unsignedTransaction, boolean wasSuccessful) {
        this.keysUsed = keysUsed;
        this.unsignedTransaction = unsignedTransaction;
        this.wasSuccessful = wasSuccessful;
    }

    public static GetUTXWithKeysResponse success(List<ECDSAPublicKey> keysUsed, Transaction unsignedTransaction) {
        try {
            return new GetUTXWithKeysResponse(
                    keysUsed,
                    ByteUtil.asByteArray(unsignedTransaction::serializeWithoutSignatures),
                    true
            );
        } catch (IOException e) {
            e.printStackTrace();
            return failure();
        }
    }

    public static GetUTXWithKeysResponse failure() {
        return new GetUTXWithKeysResponse(null, null, false);
    }


    @Override
    public void serialize(DataOutputStream outputStream) throws IOException {
        outputStream.writeBoolean(wasSuccessful);
        if (wasSuccessful) {
            CanBeSerialized.serializeList(outputStream, keysUsed);
            CanBeSerialized.serializeBytes(outputStream, unsignedTransaction);
        }
    }

    private static final class GetUTXWithKeysResponseDeserializer
            implements Deserializer<GetUTXWithKeysResponse> {

        @Override
        public GetUTXWithKeysResponse deserialize(DataInputStream inputStream) throws DeserializationException, IOException {
            boolean wasSuccessful = inputStream.readBoolean();
            if (wasSuccessful) {
                List<ECDSAPublicKey> keysUsed = Deserializer
                        .deserializeList(inputStream, ECDSAPublicKey.DESERIALIZER);
                byte[] unsignedTransaction = Deserializer.deserializeBytes(inputStream, MAX_PAYLOAD_LEN);

                return new GetUTXWithKeysResponse(keysUsed, unsignedTransaction, true);
            } else {
                return failure();
            }
        }
    }
}

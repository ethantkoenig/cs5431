package network;

import crypto.ECDSAKeyPair;
import crypto.ECDSAPublicKey;
import transaction.Transaction;
import utils.CanBeSerialized;
import utils.DeserializationException;
import utils.Deserializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class GetUTXWithKeysResponse implements CanBeSerialized {

    public final boolean wasSuccessful;
    /** Will be null if unsuccessful */
    public final List<ECDSAPublicKey> keysUsed;
    /** Will be `null` if unsuccessful */
    public final Transaction unsignedTransaction;

    public final static Deserializer<GetUTXWithKeysResponse> DESERIALIZER =
            new GetUTXWithKeysResponseDeserializer();

    private GetUTXWithKeysResponse(List<ECDSAPublicKey> keysUsed, Transaction unsignedTransaction, boolean wasSuccessful) {
        this.keysUsed = keysUsed;
        this.unsignedTransaction = unsignedTransaction;
        this.wasSuccessful = wasSuccessful;
    }

    public static GetUTXWithKeysResponse success(List<ECDSAPublicKey> keysUsed, Transaction unsignedTransaction) {
        return new GetUTXWithKeysResponse(keysUsed, unsignedTransaction, true);
    }

    public static GetUTXWithKeysResponse failure() {
        return new GetUTXWithKeysResponse(null, null, false);
    }


    @Override
    public void serialize(DataOutputStream outputStream) throws IOException {
        outputStream.writeBoolean(wasSuccessful);
        if (wasSuccessful) {
            CanBeSerialized.serializeList(outputStream, keysUsed);
            unsignedTransaction.serialize(outputStream);
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
                Transaction unsignedTransaction = Transaction.DESERIALIZER.deserialize(inputStream);

                return success(keysUsed, unsignedTransaction);
            } else {
                return failure();
            }
        }
    }
}

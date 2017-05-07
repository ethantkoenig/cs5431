package server.bodies;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import java.util.List;

@SuppressWarnings(value = "URF_UNREAD_FIELD")
public class TransactionResponseBody {
    private String payload;
    private List<String> encryptedKeys;

    public TransactionResponseBody(String payload, List<String> encryptedKeys) {
        this.payload = payload;
        this.encryptedKeys = encryptedKeys;
    }
}

package server.bodies;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import java.util.List;

@SuppressWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public class TransactionResponseBody {
    public final String payload;
    public final List<String> encryptedKeys;

    public TransactionResponseBody(String payload, List<String> encryptedKeys) {
        this.payload = payload;
        this.encryptedKeys = encryptedKeys;
    }
}

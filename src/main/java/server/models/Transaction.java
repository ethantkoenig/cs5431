package server.models;

/**
 * Created by EvanKing on 4/30/17.
 */
public class Transaction {

    private String fromuser;
    private String touser;
    private long amount;

    public Transaction(String fromuser, String touser, long amount) {
        this.fromuser = fromuser;
        this.touser = touser;
        this.amount = amount;
    }

    public String getFromuser() {
        return fromuser;
    }

    public String getTouser() {
        return touser;
    }

    public long getAmount() {
        return amount;
    }
}

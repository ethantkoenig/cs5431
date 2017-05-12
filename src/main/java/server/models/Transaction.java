package server.models;


public class Transaction {

    private int tranId;
    private String fromUser;
    private String toUser;
    private long amount;
    private String message;
    private boolean isRequest;

    public Transaction(int tranId,
                       String fromUser,
                       String toUser,
                       long amount,
                       String message,
                       boolean isRequest) {
        this.tranId = tranId;
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.amount = amount;
        this.message = message;
        this.isRequest = isRequest;
    }

    public int getTranId() {
        return tranId;
    }

    public String getFromUser() {
        return fromUser;
    }

    public String getToUser() {
        return toUser;
    }

    public long getAmount() {
        return amount;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRequest() {
        return isRequest;
    }
}

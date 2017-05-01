package server.models;


public class Transaction {

    private int tranid;
    private String fromuser;
    private String touser;
    private long amount;
    private String message;
    private boolean request;

    public Transaction(int tranid, String fromuser, String touser, long amount, String message, boolean request) {
        this.tranid = tranid;
        this.fromuser = fromuser;
        this.touser = touser;
        this.amount = amount;
        this.message = message;
        this.request = request;
    }

    public int getTranid() {
        return tranid;
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

    public String getMessage() {
        return message;
    }

    public boolean isRequest() {
        return request;
    }
}

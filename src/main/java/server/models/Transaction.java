package server.models;


public class Transaction {

    private int tranid;
    private String fromuser;
    private String touser;
    private long amount;
    private String message;
    private boolean isrequest;

    public Transaction(int tranid, String fromuser, String touser, long amount, String message, boolean isrequest) {
        this.tranid = tranid;
        this.fromuser = fromuser;
        this.touser = touser;
        this.amount = amount;
        this.message = message;
        this.isrequest = isrequest;
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
        return isrequest;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "tranid=" + tranid +
                ", fromuser='" + fromuser + '\'' +
                ", touser='" + touser + '\'' +
                ", amount=" + amount +
                ", message='" + message + '\'' +
                ", isrequest=" + isrequest +
                '}';
    }
}

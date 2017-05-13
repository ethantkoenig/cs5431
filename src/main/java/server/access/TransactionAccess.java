package server.access;

import com.google.inject.Inject;
import server.models.Transaction;
import server.utils.ConnectionProvider;
import server.utils.Statements;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class TransactionAccess extends AbstractAccess {

    @Inject
    public TransactionAccess(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    /**
     * Insert a transaction with {@code fromUser}, {@code toUser}, and {@code amount} into transactions table
     */
    public void insertTransaction(String fromUser, String toUser, long amount, String message, boolean isRequest)
            throws SQLException {
        update(Statements.insertTransaction(fromUser, toUser, amount, message, isRequest), 1);
    }

    /**
     * Get all transactions which involve the given {@code user}
     */
    public List<Transaction> getAllTransactions(String user) throws SQLException {
        return select(Statements.getAllTransactions(user), list(this::getTransaction));
    }

    /**
     * Update the transaction with {@code tranid} and {@code fromUser} to be a completed transaction (no longer a request)
     */
    public void updateTransactionRequestAsComplete(int tranId, String fromUser) throws SQLException {
        update(Statements.updateTransactionRequestAsComplete(tranId, fromUser), 1);
    }

    public List<Transaction> getRequests(String fromuser) throws SQLException {
        return select(Statements.getTransactionRequests(fromuser), list(this::getTransaction));
    }

    public void deleteRequest(int transactionId, String toUser) throws SQLException {
        update(Statements.deleteTransactionRequest(transactionId, toUser), 1);
    }

    private Transaction getTransaction(ResultSet resultSet) throws SQLException {
        int tranId = resultSet.getInt("tranid");
        String fromUser = resultSet.getString("fromuser");
        String toUser = resultSet.getString("touser");
        long amount = resultSet.getLong("amount");
        String message = resultSet.getString("message");
        boolean isRequest = resultSet.getBoolean("isrequest");
        return new Transaction(tranId, fromUser, toUser, amount, message, isRequest);
    }
}

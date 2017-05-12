package server.access;

import com.google.inject.Inject;
import server.models.Transaction;
import server.utils.ConnectionProvider;
import server.utils.Statements;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TransactionAccess extends AbstractAccess {
    private final ConnectionProvider connectionProvider;

    @Inject
    public TransactionAccess(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    /**
     * Insert a transaction with {@code fromuser}, {@code touser}, and {@code amount} into transactions table
     */
    public void insertTransaction(String fromuser, String touser, long amount, String message, boolean isrequest) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.insertTransaction(conn, fromuser, touser, amount, message, isrequest)
        ) {
            checkRowCount(preparedStmt.executeUpdate(), 1);
        }
    }

    /**
     * Get all transactions which involve the given {@code user}
     */
    public List<Transaction> getAllTransactions(String user) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.getAllTransactions(conn, user);
             ResultSet rs = preparedStmt.executeQuery()
        ) {
            List<Transaction> transactions = new ArrayList<>();
            while (rs.next()) {
                transactions.add(getTransaction(rs));
            }
            return transactions;
        }
    }

    /**
     * Update the transaction with {@code tranid} and {@code fromuser} to be a completed transaction (no longer a request)
     */
    public void updateTransactionRequestAsComplete(int tranid, String fromuser) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.updateTransactionRequestAsComplete(conn, tranid, fromuser)
        ) {
            checkRowCount(preparedStmt.executeUpdate(), 1);
        }
    }

    public List<Transaction> getRequests(String fromuser) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.getTransactionRequests(conn, fromuser);
             ResultSet rs = preparedStmt.executeQuery()
        ) {
            List<Transaction> requests = new ArrayList<>();
            while (rs.next()) {
                requests.add(getTransaction(rs));
            }
            return requests;
        }
    }

    public void deleteRequest(int transactionId, String toUser) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.deleteTransactionRequest(conn, transactionId, toUser)
        ) {
            preparedStmt.executeUpdate();
        }
    }

    private Transaction getTransaction(ResultSet rs) throws SQLException {
        int tranid = rs.getInt("tranid");
        String fromuser = rs.getString("fromuser");
        String touser = rs.getString("touser");
        long amount = rs.getLong("amount");
        String message = rs.getString("message");
        boolean isrequest = rs.getBoolean("isrequest");
        return new Transaction(tranid, fromuser, touser, amount, message, isrequest);
    }
}

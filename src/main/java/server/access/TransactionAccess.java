package server.access;

import com.google.inject.Inject;
import server.models.Transaction;
import server.utils.ConnectionProvider;
import server.utils.Statements;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class TransactionAccess {
    private static final Logger LOGGER = Logger.getLogger(TransactionAccess.class.getName());

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
             PreparedStatement preparedStmt = Statements.insertTransaction(conn, fromuser, touser, amount, message, isrequest);
        ) {
            int rowCount = preparedStmt.executeUpdate();
            if (rowCount != 1) {
                String msg = String.format("Update affected %d rows, expected 1", rowCount);
                LOGGER.severe(msg);
            }
        }
    }

    public List<Transaction> getAllTransactions(String user) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.getAllTransactions(conn, user);
             ResultSet rs = preparedStmt.executeQuery();
        ) {
            List<Transaction> transactions = new ArrayList<>();
            while (rs.next()) {
                int tranid = rs.getInt("tranid");
                String fromuser = rs.getString("fromuser");
                String touser = rs.getString("touser");
                long amount = rs.getLong("amount");
                String message = rs.getString("message");
                boolean isrequest = rs.getBoolean("isrequest");
                transactions.add(new Transaction(tranid, fromuser, touser, amount, message, isrequest));
            }
            return transactions;
        }
    }

    public void updateTransactionRequestAsComplete(int tranid, String fromuser) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.updateTransactionRequestAsComplete(conn, tranid, fromuser)
        ) {
            int rowCount = preparedStmt.executeUpdate();
            if (rowCount != 1) {
                String msg = String.format("Update affected %d rows, expected 1", rowCount);
                LOGGER.severe(msg);
            }
        }
    }

    public List<Transaction> getRequests(String fromuser) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.getTransactionRequests(conn, fromuser);
             ResultSet rs = preparedStmt.executeQuery();
        ) {
            List<Transaction> requests = new ArrayList<>();
            while (rs.next()) {
                int tranid = rs.getInt("tranid");
                String touser = rs.getString("touser");
                long amount = rs.getLong("amount");
                String message = rs.getString("message");
                boolean isrequest = rs.getBoolean("isrequest");
                requests.add(new Transaction(tranid, fromuser, touser, amount, message, isrequest));
            }
            return requests;
        }
    }
}

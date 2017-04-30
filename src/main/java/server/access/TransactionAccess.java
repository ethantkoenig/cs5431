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
    public void insertTransaction(String fromuser, String touser, long amount) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.insertTransaction(conn, fromuser, touser, amount);
        ) {
            int rowCount = preparedStmt.executeUpdate();
            if (rowCount != 1) {
                String msg = String.format("Update affected %d rows, expected 1", rowCount);
                LOGGER.severe(msg);
            }
        }
    }

    public List<Transaction> getAllTransactions() throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(Statements.GET_ALL_TRANSACTIONS);
        ) {
            List<Transaction> transactions = new ArrayList<>();
            while (rs.next()) {
                String fromuser = rs.getString("fromuser");
                String touser = rs.getString("touser");
                long amount = rs.getLong("amount");
                transactions.add(new Transaction(fromuser, touser, amount));
            }
            return transactions;
        }
    }

}

package server.config;

import com.google.inject.Inject;
import server.utils.ConnectionProvider;
import server.utils.Statements;
import utils.Log;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Class to config and initialize DB with necessary tables. So far only creates a users table.
 * NOTE: unable to run sql scripts because of findbugs and dependency restrictions. See line 31.
 *
 * @version 1.0, March 11 2017
 */
public final class DatabaseConfig {
    private final ConnectionProvider connectionProvider;

    @Inject
    public DatabaseConfig(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    private static final Log LOGGER = Log.forClass(DatabaseConfig.class);

    public void dbInit() {
        LOGGER.info("[!] Initializing database");
        try (Connection connection = connectionProvider.getConnection()) {
            setUp(connection);
        } catch (SQLException e) {
            LOGGER.severe(e.getMessage());
        }
    }

    public static void setUp(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(Statements.CREATE_USERS_TABLE);
            statement.executeUpdate(Statements.CREATE_KEYS_TABLE);
            statement.executeUpdate(Statements.CREATE_PASSWORD_RECOVERY_TABLE);
            statement.executeUpdate(Statements.CREATE_FRIENDS_TABLE);
            statement.executeUpdate(Statements.CREATE_TRANSACTIONS_TABLE);
            statement.executeUpdate(Statements.CREATE_PENDING_KEYS_TABLE);
        }
    }
}
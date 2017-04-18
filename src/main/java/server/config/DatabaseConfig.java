package server.config;

import com.google.inject.Inject;
import server.utils.ConnectionProvider;
import server.utils.Statements;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

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

    private static final Logger LOGGER = Logger.getLogger(DatabaseConfig.class.getName());

    public void dbInit() {
        LOGGER.info("[!] Initializing database");
        try (
                Connection connection = connectionProvider.getConnection();
                Statement statement = connection.createStatement()
        ) {
            int result = statement.executeUpdate(Statements.SHOW_DB_LIKE);
            if (result == 0) { // database does not exist
                LOGGER.info("[+] Creating database " + Statements.DB_NAME);
                createDB(statement);
                createTables(statement);
            } else {
                LOGGER.info("[!] Database already created: " + Statements.DB_NAME);
            }
        } catch (SQLException e) {
            LOGGER.severe(e.getMessage());
        }
    }

    public static void createDB(Statement statement) throws SQLException {
        statement.executeUpdate(Statements.CREATE_DB);
        statement.executeUpdate(Statements.USE_DB);
    }

    public static void createTables(Statement statement) throws SQLException {
        statement.executeUpdate(Statements.CREATE_USERS_TABLE);
        statement.executeUpdate(Statements.CREATE_KEYS_TABLE);
        statement.executeUpdate(Statements.CREATE_PASSWORD_RECOVERY_TABLE);
        statement.executeUpdate(Statements.CREATE_FRIENDS_TABLE);
    }
}
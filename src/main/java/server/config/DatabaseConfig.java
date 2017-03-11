package server.config;

import server.utils.DbUtil;
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
public class DatabaseConfig {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConfig.class.getName());

    public static void dbInit() {
        Connection connection = null;
        Statement statement = null;
        try {
            LOGGER.info("[!] Initializing database");
            connection = DbUtil.getConnection(true);
            statement = connection.createStatement();
            int result = statement.executeUpdate(Statements.SHOW_DB_LIKE);
            // The user does not already have this db.
            if (result == 0) {
                //DbUtil.runScript(connection, "dbconfig.sql");
                LOGGER.info("[+] Creating database " + Statements.DB_NAME);
                createDB(statement);
                createUserTable(statement);
            } else {
                LOGGER.info("[!] Database already created: " + Statements.DB_NAME);
            }
        } catch (Exception e) {
            LOGGER.severe(e.getMessage());
        } finally {
            try {
                statement.close();
            } catch (Exception e) {
                LOGGER.severe(e.getMessage());
            }
            try {
                connection.close();
            } catch (Exception e) {
                LOGGER.severe(e.getMessage());
            }
        }
    }

    public static void createDB(Statement statement) throws SQLException {
        statement.executeUpdate(Statements.CREATE_DB);
        statement.executeUpdate(Statements.USE_DB);
    }

    public static void createUserTable(Statement statement) throws SQLException {
        statement.executeUpdate(Statements.CREATE_USERS_TABLE);
        statement.executeUpdate(Statements.INITIAL_INSERT);
    }
}
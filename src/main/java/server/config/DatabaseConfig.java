package server.config;

import server.utils.DbUtil;
import server.utils.Statements;

import java.sql.Connection;
import java.sql.Statement;
import java.util.logging.Logger;

public class DatabaseConfig {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConfig.class.getName());
    private static String dbName = "yaccoin";


    public static void dbInit() {
        Connection connection = null;
        Statement statement = null;
        try {
            LOGGER.info("[!] Initializing database");
            connection = DbUtil.getConnection();
            statement = connection.createStatement();
            int result = statement.executeUpdate(Statements.SHOW_DB_LIKE);
            // The user does not already have this db.
            if (result == 0) {
                //TODO: cant run script without findbug flipping out
                //DbUtil.runScript(connection, "dbconfig.sql");
                LOGGER.info("[+] Creating database " + dbName);
                statement.executeUpdate(Statements.CREATE_DB);
                statement.executeUpdate(Statements.USE_DB);
                statement.executeUpdate(Statements.CREATE_TABLE);
                statement.executeUpdate(Statements.INITIAL_INSERT);
            } else {
                LOGGER.info("[!] Database already created: " + dbName);
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
}
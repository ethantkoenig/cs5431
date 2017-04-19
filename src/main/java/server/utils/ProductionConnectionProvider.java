package server.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

public final class ProductionConnectionProvider implements ConnectionProvider {

    private static final Logger LOGGER = Logger.getLogger(ProductionConnectionProvider.class.getName());
    private static final String jdbcDriver = "com.mysql.jdbc.Driver";
    private static final String dbHost = "50.159.66.236:1234";
    private static final String dbUser = "cs5431";
    private static final String dbPassword = System.getenv("MYSQL_PASS");


    @Override
    public Connection getConnection() {
        Connection connection = null;
        try {
            Class.forName(jdbcDriver);
            //TODO: don't hard code this string in here
            String url = String.format("jdbc:mysql://%s/%s?user=%s&password=%s",
                    dbHost, Statements.DB_NAME, dbUser, dbPassword);
            connection = DriverManager.getConnection(url);
        } catch (ClassNotFoundException | SQLException e) {
            LOGGER.severe("Unable to connect to DB, make sure you added MYSQL_PASS to env variables: " + e.getMessage());
        }
        return connection;
    }
}

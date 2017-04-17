package server.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

public final class ProductionConnectionProvider implements ConnectionProvider {

    private static final Logger LOGGER = Logger.getLogger(ProductionConnectionProvider.class.getName());
    private static final String jdbcDriver = "com.mysql.jdbc.Driver";
    private static final String dbPassword = System.getenv("MYSQL_PASS");

    @Override
    public Connection getConnection() {
        Connection connection = null;
        try {
            Class.forName(jdbcDriver);
            //TODO: don't hard code this string in here
            connection = DriverManager.getConnection("jdbc:mysql://localhost/" + Statements.DB_NAME + "?user=root&password=" + dbPassword);
        } catch (ClassNotFoundException | SQLException e) {
            LOGGER.severe("Unable to connect to DB, make sure you added MYSQL_PASS to env variables: " + e.getMessage());
        }
        return connection;
    }
}

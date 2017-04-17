package server.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

public final class DbUtil {

    private static final Logger LOGGER = Logger.getLogger(DbUtil.class.getName());
    private static final String jdbcDriver = "com.mysql.jdbc.Driver";
    private static final String dbPassword = System.getenv("MYSQL_PASS");

    // Disallow instances of this class
    private DbUtil() {
    }

    public static Connection getConnection(Boolean initial) {
        Connection connection = null;
        String db = "";
        try {
            Class.forName(jdbcDriver);
            if (!initial) db = Statements.DB_NAME;
            //TODO: don't hard code this string in here
            connection = DriverManager.getConnection("jdbc:mysql://50.159.66.236:1234/" + db + "?user=cs5431&password=" + dbPassword);
        } catch (ClassNotFoundException | SQLException e) {
            LOGGER.severe("Unable to connect to DB, make sure you added MYSQL_PASS to env variables: " + e.getMessage());
        }
        return connection;
    }

}

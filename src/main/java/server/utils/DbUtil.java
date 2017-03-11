package server.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Created by EvanKing on 3/10/17.
 */
public class DbUtil {

    private static final Logger LOGGER = Logger.getLogger(DbUtil.class.getName());
    private static final String STATEMENT_DELIMITER = ";";
    private static final String SCRIPT_PATH = "src/main/resources/sql/";
    private static String jdbcDriver = "com.mysql.jdbc.Driver";
    private static String dbPassword = "";


//    public static void runScript(Connection conn, String scriptPath) throws IOException, SQLException {
//        BufferedReader in = null;
//        Statement statement = null;
//        try {
//            StringBuilder command = new StringBuilder();
//            in = new BufferedReader(new InputStreamReader(new FileInputStream(SCRIPT_PATH + "dbconfig.sql"), Charset.defaultCharset()));
//            statement = conn.createStatement();
//            for (String line = in.readLine(); line != null; line = in.readLine()) {
//                System.out.println(line);
//                command.append(line);
//                if (line.contains(STATEMENT_DELIMITER)) {
//                    System.out.println(command);
//                    statement.executeUpdate(command.toString());
//                    command.setLength(0);
//                }
//            }
//        }finally{
//            in.close();
//            statement.close();
//        }
//    }


    public static Connection getConnection() {
        Connection connection = null;
        try {
            Class.forName(jdbcDriver);
            connection = DriverManager.getConnection("jdbc:mysql://localhost/?user=root&password=" + dbPassword);
        } catch (ClassNotFoundException | SQLException e) {
            LOGGER.severe("Unable to connect to DB, check that you added your password: " + e.getMessage());
        }
        return connection;
    }
}

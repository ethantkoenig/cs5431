package server.access;

import server.utils.DbUtil;
import server.utils.Statements;

import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.sql.*;
import java.util.logging.Logger;

import static utils.ShaTwoFiftySix.hashOf;

public class PasswordRecoveryAccess {

    private static final Logger LOGGER = Logger.getLogger(PasswordRecoveryAccess.class.getName());

    // Disallow instances of this class
    private PasswordRecoveryAccess() {
    }

    /**
     * Check if guid exists in a record in the passrecover table and return the associate userid or -1
     */
    public static int getPasswordRecoveryUserID(String GUID) throws SQLException, GeneralSecurityException {
        String GUIDHash = hashOf(GUID.getBytes(Charset.forName("UTF-8"))).toString();
        try (Connection conn = DbUtil.getConnection(false);
             PreparedStatement preparedStmt = Statements.getPasswordRecoveryUserID(conn,GUIDHash);
             ResultSet rs = preparedStmt.executeQuery()
        ) {
            if (rs.next()) {
                int userid = rs.getInt("userid");
                Date dt = rs.getDate("dt");

                return userid;
            }
            return -1;
        }
    }

    /**
     * Add ta row to password recovery table with userid, current time, and hashed guid
     */
    public static void insertPasswordRecovery(int userID, String GUID) throws SQLException, GeneralSecurityException {
        String GUIDHash = hashOf(GUID.getBytes(Charset.forName("UTF-8"))).toString();
        try (Connection conn = DbUtil.getConnection(false);
             PreparedStatement preparedStmt = Statements.insertPasswordRecovery(conn, userID, GUIDHash)
        ) {
            int rowCount = preparedStmt.executeUpdate();
            if (rowCount != 1) {
                String msg = String.format("Insert affected %d rows, expected 1", rowCount);
                LOGGER.severe(msg);
            }
        }
    }

}

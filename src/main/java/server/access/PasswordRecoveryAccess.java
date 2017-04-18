package server.access;

import com.google.inject.Inject;
import server.utils.ConnectionProvider;
import server.utils.Statements;

import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.OptionalInt;
import java.util.logging.Logger;

import static utils.ShaTwoFiftySix.hashOf;

public class PasswordRecoveryAccess {
    private static final Logger LOGGER = Logger.getLogger(PasswordRecoveryAccess.class.getName());

    private final ConnectionProvider connectionProvider;

    @Inject
    public PasswordRecoveryAccess(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    /**
     * Check if guid exists in a record in the recovery table and return the associated userID
     */
    public OptionalInt getPasswordRecoveryUserID(String GUID) throws SQLException, GeneralSecurityException {
        String GUIDHash = hashOf(GUID.getBytes(Charset.forName("UTF-8"))).toString();
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.getPasswordRecoveryUserID(conn,GUIDHash);
             ResultSet rs = preparedStmt.executeQuery()
        ) {
            if (rs.next()) {
                return OptionalInt.of(rs.getInt("userid"));
            }
            return OptionalInt.empty();
        }
    }

    /**
     * Add a row to the recovery table with userid, current time, and hashed guid
     */
    public void insertPasswordRecovery(int userID, String GUID) throws SQLException, GeneralSecurityException {
        String GUIDHash = hashOf(GUID.getBytes(Charset.forName("UTF-8"))).toString();
        try (Connection conn = connectionProvider.getConnection();
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

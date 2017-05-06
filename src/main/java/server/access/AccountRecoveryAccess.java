package server.access;

import com.google.inject.Inject;
import server.models.User;
import server.utils.ConnectionProvider;
import server.utils.Statements;

import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.logging.Logger;

public class AccountRecoveryAccess extends AbstractAccess {
    private static final Logger LOGGER = Logger.getLogger(AccountRecoveryAccess.class.getName());

    private final ConnectionProvider connectionProvider;
    private final UserAccess userAccess;

    @Inject
    public AccountRecoveryAccess(ConnectionProvider connectionProvider, UserAccess userAccess) {
        this.connectionProvider = connectionProvider;
        this.userAccess = userAccess;
    }

    /**
     * Check if guid exists in a record in the recovery table and return the associated userID
     */
    public OptionalInt getUserIdByGUID(String guid) throws SQLException, GeneralSecurityException {
        String guidHash = hashOfGuid(guid);
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.getPasswordRecoveryUserID(conn, guidHash);
             ResultSet rs = preparedStmt.executeQuery()
        ) {
            if (rs.next()) {
                return OptionalInt.of(rs.getInt("userid"));
            }
            return OptionalInt.empty();
        }
    }

    public Optional<User> getUserByGUID(String guid) throws SQLException, GeneralSecurityException {
        OptionalInt optUserID = getUserIdByGUID(guid);
        if (!optUserID.isPresent()) {
            return Optional.empty();
        }
        Optional<User> user = userAccess.getUserByID(optUserID.getAsInt());
        if (!user.isPresent()) {
            LOGGER.severe("Unknown user associated with recognized GUID");
        }
        return user;
    }

    /**
     * Add a row to the recovery table with userid, current time, and hashed guid
     */
    public void insertRecovery(int userID, String guid) throws SQLException, GeneralSecurityException {
        String guidHash = hashOfGuid(guid);
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.insertPasswordRecovery(conn, userID, guidHash)
        ) {
            checkRowCount(preparedStmt.executeUpdate(), 1);
        }
    }

    public void deleteRecovery(String guid) throws SQLException, GeneralSecurityException {
        String guidHash = hashOfGuid(guid);
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.deletePasswordRecovery(conn, guidHash)
        ) {
            checkRowCount(preparedStmt.executeUpdate(), 1);
        }
    }
}

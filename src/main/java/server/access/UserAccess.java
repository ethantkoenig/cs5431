package server.access;

import com.google.inject.Inject;
import server.models.User;
import server.utils.ConnectionProvider;
import server.utils.Statements;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * The layer between user objects and the "users" table in the DB.
 * Utilities for reading and modifying database.
 */
public final class UserAccess extends AbstractAccess {

    @Inject
    public UserAccess(ConnectionProvider connectionProvider) {
        super(connectionProvider);
    }

    public List<String> getAllUsernames() throws SQLException {
        return select(conn -> conn.prepareStatement(Statements.GET_ALL_USERS),
                list(resultSet -> resultSet.getString("username")));
    }

    public Optional<User> getUserByID(int userID) throws SQLException {
        return select(Statements.selectUserByID(userID), optional(this::getUser));
    }

    public Optional<User> getUserByUsername(String username) throws SQLException {
        return select(Statements.selectUserByUsername(username), optional(this::getUser));
    }

    public Optional<User> getUserByEmail(String email) throws SQLException {
        return select(Statements.selectUserByEmail(email), optional(this::getUser));
    }

    public void insertUser(String username, String email, byte[] salt, byte[] hashedPassword) throws SQLException {
        update(Statements.insertUser(username, email, salt, hashedPassword), 1);
    }

    public void updateUserPass(int userID, byte[] salt, byte[] hashedPassword) throws SQLException {
        update(Statements.updateUserPassword(userID, salt, hashedPassword), 1);
    }


    public void incrementFailedLogins(int userID) throws SQLException {
        update(Statements.incrementFailedLogins(userID), 1);
    }


    public void resetFailedLogins(int userID) throws SQLException {
        update(Statements.resetFailedLogins(userID), 1);
    }


    public boolean isFriendsWith(String username, String friend) throws SQLException {
        return select(Statements.isFriendsWith(username, friend), ResultSet::next);
    }


    public void insertFriends(String username, String friend) throws SQLException {
        update(Statements.insertFriends(username, friend), 1);
    }


    public void deleteFriends(String username, String friend) throws SQLException {
        update(Statements.deleteFriends(username, friend), 1);
    }


    public List<String> getFriends(String username) throws SQLException {
        return select(Statements.getFriends(username),
                list(resultSet -> resultSet.getString("friend")));
    }

    public List<String> getPeopleWhoFriendMe(String username) throws SQLException {
        return select(Statements.getPeopleWhoFriendMe(username),
                list(resultSet -> resultSet.getString("username")));
    }

    private User getUser(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        String username = resultSet.getString("username");
        byte[] salt = resultSet.getBytes("salt");
        byte[] hashedPassword = resultSet.getBytes("pass");
        String email = resultSet.getString("email");
        int failedLogins = resultSet.getInt("failedLogins");
        return new User(id, username, email, salt, hashedPassword, failedLogins);
    }
}

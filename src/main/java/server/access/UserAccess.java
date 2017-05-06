package server.access;

import com.google.inject.Inject;
import server.models.User;
import server.utils.ConnectionProvider;
import server.utils.Statements;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The layer between user objects and the "users" table in the DB.
 * Utilities for reading and modifying database.
 */
public final class UserAccess extends AbstractAccess {
    private final ConnectionProvider connectionProvider;

    @Inject
    public UserAccess(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public List<String> getAllUsernames() throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(Statements.GET_ALL_USERS)
        ) {
            List<String> users = new ArrayList<>();
            while (rs.next()) {
                users.add(rs.getString("username"));
            }
            return users;
        }
    }

    public Optional<User> getUserByID(int userID) throws SQLException {
        try (
                Connection conn = connectionProvider.getConnection();
                PreparedStatement preparedStmt = Statements.selectUserByID(conn, userID);
                ResultSet rs = preparedStmt.executeQuery()
        ) {
            if (rs.next()) {
                return Optional.of(getUser(rs));
            }
            return Optional.empty();
        }
    }

    public Optional<User> getUserByUsername(String username) throws SQLException {
        try (
                Connection conn = connectionProvider.getConnection();
                PreparedStatement preparedStmt = Statements.selectUserByUsername(conn, username);
                ResultSet rs = preparedStmt.executeQuery()
        ) {
            if (rs.next()) {
                return Optional.of(getUser(rs));
            }
            return Optional.empty();
        }
    }

    public Optional<User> getUserByEmail(String email) throws SQLException {
        try (
                Connection conn = connectionProvider.getConnection();
                PreparedStatement preparedStmt = Statements.selectUserByEmail(conn, email);
                ResultSet rs = preparedStmt.executeQuery()
        ) {
            if (rs.next()) {
                return Optional.of(getUser(rs));
            }
            return Optional.empty();
        }
    }

    public void insertUser(String username, String email, byte[] salt, byte[] hashedPassword) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.insertUser(conn, username, email, salt, hashedPassword)
        ) {
            checkRowCount(preparedStmt.executeUpdate(), 1);
        }
    }


    public void updateUserPass(int userID, byte[] salt, byte[] hashedPassword) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.updateUserPassword(conn, userID, salt, hashedPassword)
        ) {
            checkRowCount(preparedStmt.executeUpdate(), 1);
        }
    }


    public void incrementFailedLogins(int userID) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.incrementFailedLogins(conn, userID)
        ) {
            checkRowCount(preparedStmt.executeUpdate(), 1);
        }
    }


    public void resetFailedLogins(int userID) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.resetFailedLogins(conn, userID)
        ) {
            checkRowCount(preparedStmt.executeUpdate(), 1);
        }
    }


    public boolean isFriendsWith(String username, String friend) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.isFriendsWith(conn, username, friend);
             ResultSet rs = preparedStmt.executeQuery()
        ) {
            if (rs.next()) {
                return true;
            }
        }
        return false;
    }


    public void insertFriends(String username, String friend) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.insertFriends(conn, username, friend)
        ) {
            checkRowCount(preparedStmt.executeUpdate(), 1);
        }
    }


    public void deleteFriends(String username, String friend) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.deleteFriends(conn, username, friend)
        ) {
            checkRowCount(preparedStmt.executeUpdate(), 1);
        }
    }


    public List<String> getFriends(String username) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.getFriends(conn, username);
             ResultSet rs = preparedStmt.executeQuery()
        ) {
            List<String> friends = new ArrayList<>();
            while (rs.next()) {
                friends.add(rs.getString("friend"));
            }
            return friends;
        }
    }

    public List<String> getPeopleWhoFriendMe(String username) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement preparedStmt = Statements.getPeopleWhoFriendMe(conn, username);
             ResultSet rs = preparedStmt.executeQuery()
        ) {
            List<String> usernames = new ArrayList<>();
            while (rs.next()) {
                usernames.add(rs.getString("username"));
            }
            return usernames;
        }
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

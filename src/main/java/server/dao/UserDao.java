package server.dao;

import server.models.User;
import server.utils.DbUtil;
import server.utils.Statements;
import utils.Crypto;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.sql.*;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Created by EvanKing on 3/10/17.
 */
public class UserDao {

    private static final Logger LOGGER = Logger.getLogger(UserDao.class.getName());

    public ArrayList<User> getUsers() {
        return null;
    }

    public User getUserbyUsername(String username) throws SQLException {
        Connection conn = null;
        PreparedStatement preparedStmt = null;
        User user = null;
        ResultSet rs = null;
        try {
            conn = DbUtil.getConnection(false);
            preparedStmt = conn.prepareStatement(Statements.SELECT_USER_BY_USERNAME);
            preparedStmt.setString(1, username);
            rs = preparedStmt.executeQuery();
            PublicKey publicKey = null;
            if (rs.next()) {
                int userid = rs.getInt("userid");
                byte[] key = rs.getBytes("publickey");
                if(key != null) publicKey = Crypto.deserializePublicKey(ByteBuffer.wrap(key));
                user = new User(userid,  username, publicKey);
            }
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } finally {
            try {
                rs.close();
            } catch (Exception e) {
                LOGGER.severe(e.getMessage());
            }
            try {
                preparedStmt.close();
            } catch (Exception e) {
                LOGGER.severe(e.getMessage());
            }
            try {
                conn.close();
            } catch (Exception e) {
                LOGGER.severe(e.getMessage());
            }
        }
        return user;
    }

    public void registerUser(User user) {
        return;
    }
}

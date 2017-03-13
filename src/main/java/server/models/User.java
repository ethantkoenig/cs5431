package server.models;

import java.security.PublicKey;

public class User {

    private int userid;
    private String username;

    public User(int userid, String username) {
        // TODO username uniqueness
        this.userid = userid;
        this.username = username;
    }

    public int getUserid() {
        return userid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        // TODO check for uniqueness
        this.username = username;
    }
}

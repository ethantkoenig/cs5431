package server.models;

import java.security.PublicKey;

public class User {

    private int userid;
    private String username;
    private PublicKey publicKey;

    public User(int userid, String username, PublicKey publicKey) {
        // TODO username uniqueness
        this.userid = userid;
        this.username = username;
        this.publicKey = publicKey;
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

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }
}

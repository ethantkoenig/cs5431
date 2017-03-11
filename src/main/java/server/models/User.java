package server.models;

import java.security.PublicKey;

/**
 * Created by EvanKing on 3/10/17.
 */
public class User {

    private int userid;
    private String username;
    private PublicKey publicKey;

    public User(int userid, String username, PublicKey publicKey) {
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
        this.username = username;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public String serialize(){
        //TODO: I know this will be a big fight with our group...
        return null;
    }
}

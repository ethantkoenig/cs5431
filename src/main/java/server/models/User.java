package server.models;

import java.util.Arrays;

public class User {

    private final int id;
    private String username;
    private byte[] salt;
    private byte[] hashedPassword;

    public User(int id, String username, byte[] salt, byte[] hashedPassword) {
        // TODO username uniqueness
        this.id = id;
        this.username = username;
        this.salt = Arrays.copyOf(salt, salt.length);
        this.hashedPassword = Arrays.copyOf(hashedPassword, hashedPassword.length);
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        // TODO check for uniqueness
        this.username = username;
    }

    public byte[] getSalt() {
        return Arrays.copyOf(salt, salt.length);
    }

    public byte[] getHashedPassword() {
        return Arrays.copyOf(hashedPassword, hashedPassword.length);
    }
}

package server.models;

import java.util.Arrays;

public class User {

    private final int id;
    private String username;
    private byte[] salt;
    private byte[] hashedPassword;
    private String email;

    public User(int id, String username, String email, byte[] salt, byte[] hashedPassword) {
        this.id = id;
        this.username = username;
        this.salt = Arrays.copyOf(salt, salt.length);
        this.hashedPassword = Arrays.copyOf(hashedPassword, hashedPassword.length);
        this.email = email;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public byte[] getSalt() {
        return Arrays.copyOf(salt, salt.length);
    }

    public byte[] getHashedPassword() {
        return Arrays.copyOf(hashedPassword, hashedPassword.length);
    }

    public String getEmail() {
        return email;
    }
}

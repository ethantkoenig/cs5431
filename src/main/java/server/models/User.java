package server.models;

import crypto.Crypto;

import java.util.Arrays;

public class User {

    private final int id;
    private String username;
    private byte[] salt;
    private byte[] hashedPassword;
    private String email;
    private int failedLogins;

    public User(int id, String username, String email, byte[] salt, byte[] hashedPassword, int failedLogins) {
        this.id = id;
        this.username = username;
        this.salt = Arrays.copyOf(salt, salt.length);
        this.hashedPassword = Arrays.copyOf(hashedPassword, hashedPassword.length);
        this.email = email;
        this.failedLogins = failedLogins;
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

    public int getFailedLogins() {
        return failedLogins;
    }

    public boolean checkPassword(String password) throws Exception {
        return Arrays.equals(Crypto.hashAndSalt(password, salt), hashedPassword);
    }
}

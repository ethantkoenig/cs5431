package server.utils;

// TODO: probably doesnt belong in utils but not sure where else to put it as of yet
public class Statements {

    private static final String DB_NAME = "yaccoin";
    public static final String CREATE_DB = "CREATE DATABASE yaccoin";
    public static final String USE_DB = "USE yaccoin";
    public static final String CREATE_TABLE = "CREATE TABLE user ( userid int NOT NULL AUTO_INCREMENT, username varchar(100) NOT NULL, pass varchar(30) NOT NULL, publickey varchar(100) DEFAULT NULL, PRIMARY KEY (userid) )";

    //TODO: this will be removed. Just for testing.
    public static final String INITIAL_INSERT = "INSERT INTO user (username, pass) VALUES ('Evan','password')";
    public static final String SHOW_DB_LIKE = String.format("SHOW DATABASES LIKE '%s'",DB_NAME);

}

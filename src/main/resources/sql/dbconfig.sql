CREATE DATABASE yaccoin;
USE yaccoin;

CREATE TABLE user (
userid int NOT NULL AUTO_INCREMENT,
username varchar(100) NOT NULL,
pass varchar(30) NOT NULL,
publickey varchar(100) DEFAULT NULL,
PRIMARY KEY (userid)
);

INSERT INTO user (username, pass) VALUES ('Evan','password');
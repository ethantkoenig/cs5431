package server;

import server.config.DatabaseConfig;
import server.controllers.IndexController;
import server.controllers.UserController;
import server.dao.UserDao;

import static spark.Spark.port;
import static spark.Spark.staticFiles;


public class Application {

    public static void main(String args[]) {

        UserDao userDao = new UserDao();

        // Configure Spark on port 5000
        port(5000);
        // Static files location
        staticFiles.location("/public");
        // Caching of static files lifetime
        staticFiles.expireTime(600L);

        DatabaseConfig.dbInit();
        IndexController.serveIndexPage();
        UserController.startUserController(userDao);
    }
}
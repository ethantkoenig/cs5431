package server;

import server.Controllers.IndexController;

import static spark.Spark.port;
import static spark.Spark.staticFiles;


public class Application {

    public static void main(String args[]) {

        // Configure Spark on port 5000
        port(5000);
        // Static files location
        staticFiles.location("/public");
        // Caching of static files lifetime
        staticFiles.expireTime(600L);

        IndexController.serveIndexPage();

    }

}
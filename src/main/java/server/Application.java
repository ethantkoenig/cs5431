package server;

import server.config.DatabaseConfig;
import server.controllers.IndexController;
import server.controllers.TransactionController;
import server.controllers.UserController;
import server.utils.Constants;

import java.net.InetSocketAddress;

import static spark.Spark.port;
import static spark.Spark.staticFiles;


public class Application {

    public static void run(InetSocketAddress nodeAddress) {
        Constants.setNodeAddress(nodeAddress);

        // Configure Spark on port 5000
        port(5000);
        // Static files location
        staticFiles.location("/public");
        // Caching of static files lifetime
        staticFiles.expireTime(600L);

        DatabaseConfig.dbInit();
        IndexController.serveIndexPage();
        UserController.startUserController();
        TransactionController.makeTransaction();
    }
}

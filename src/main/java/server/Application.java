package server;

import server.config.DatabaseConfig;
import server.controllers.IndexController;
import server.controllers.PasswordRecoveryController;
import server.controllers.TransactionController;
import server.controllers.UserController;
import server.utils.Constants;
import utils.IOUtils;

import java.net.InetSocketAddress;
import java.util.Optional;

import static spark.Spark.*;


public class Application {

    public static boolean run(String[] args) {
//        if (!handleArgs(args)) {
//            return false;
//        }

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
        PasswordRecoveryController.recoverPassword();
        return true;
    }

    private static boolean handleArgs(String args[]) {
        if (args.length != 8) {
            System.err.println("usage: webserver <keystore> <ip-address>:<port> <node args>");
            return false;
        }

        String keystorePath = args[1];
        Optional<InetSocketAddress> optAddr = IOUtils.parseAddress(args[2]);
        if (!optAddr.isPresent()) {
            System.err.println("Invalid address: " + args[2]);
            return false;
        }
        Constants.setNodeAddress(optAddr.get());

        String keystorePassword = System.getenv("KEYSTORE_PASS");
        if (keystorePassword == null) {
            System.err.println("Store keystore password in $KEYSTORE_PASS env variable");
            return false;
        }
        secure(keystorePath, keystorePassword, null, null);
        return true;
    }
}

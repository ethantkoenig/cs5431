package server;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import server.access.DatabaseUserAccess;
import server.access.UserAccess;
import server.config.DatabaseConfig;
import server.controllers.IndexController;
import server.controllers.PasswordRecoveryController;
import server.controllers.TransactionController;
import server.controllers.UserController;
import server.utils.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import static spark.Spark.*;


public class Application {

    public static boolean run(String[] args) {
        if (!handleArgs(args)) {
            return false;
        }

        // Configure Spark on port 5000
        port(5000);
        // Static files location
        staticFiles.location("/public");
        // Caching of static files lifetime
        staticFiles.expireTime(600L);

        Injector injector = Guice.createInjector(new Module());
        injector.getInstance(DatabaseConfig.class).dbInit();

        injector.getInstance(IndexController.class).init();
        injector.getInstance(UserController.class).init();
        injector.getInstance(TransactionController.class).init();
        injector.getInstance(PasswordRecoveryController.class).init();

        return true;
    }

    private static boolean handleArgs(String args[]) {
        if (args.length != 7) {
            System.err.println("usage: webserver <keystore> <port> <public-key> <private-key> <privileged-key> <File for list of nodes>");
            return false;
        }

        String keystorePath = args[1];
        try {
            int port = Integer.parseInt(args[2]);
            Constants.setNodeAddress(new InetSocketAddress(InetAddress.getLocalHost(), port));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (NumberFormatException e) {
            System.err.println("Misformatted port number: " + args[2]);
            return false;
        }

        String keystorePassword = System.getenv("KEYSTORE_PASS");
        if (keystorePassword == null) {
            System.err.println("Store keystore password in $KEYSTORE_PASS env variable");
            return false;
        }
        secure(keystorePath, keystorePassword, null, null);
        return true;
    }

    private static class Module extends AbstractModule {
        @Override
        protected void configure() {
            bind(UserAccess.class).to(DatabaseUserAccess.class);
            bind(ConnectionProvider.class).to(ProductionConnectionProvider.class);
            bind(MailService.class).to(GmailService.class);
        }
    }
}

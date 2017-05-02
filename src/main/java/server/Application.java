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
import utils.IOUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Properties;

import static spark.Spark.*;


public class Application {

    public static boolean run(Properties serverProp) {
        if (!handleArgs(serverProp)) {
            return false;
        }

        int serverPort = Integer.parseInt(IOUtils.getPropertyChecked(serverProp, "serverPort"));
        // Configure Spark on port `port`
        port(serverPort);
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

    private static boolean handleArgs(Properties serverProp) {
        String keystorePath = IOUtils.getPropertyChecked(serverProp, "keystore");
        try {
            String host = IOUtils.getPropertyChecked(serverProp, "nodeAddress");
            int port = Integer.parseInt(IOUtils.getPropertyChecked(serverProp, "nodePort"));
            Constants.setNodeAddress(new InetSocketAddress(host, port));
        } catch (NumberFormatException e) {
            System.err.println("Misformatted port number: " + IOUtils.getPropertyChecked(serverProp, "nodePort"));
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

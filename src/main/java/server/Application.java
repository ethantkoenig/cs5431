package server;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import server.config.DatabaseConfig;
import server.controllers.*;
import server.utils.*;
import utils.IOUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

import static spark.Spark.*;


public class Application {

    public static boolean run(Properties serverProp) {
        if (!handleArgs(serverProp)) {
            return false;
        }

        int serverPort;
        try {
            serverPort = Integer.parseInt(IOUtils.getPropertyChecked(serverProp, "serverPort"));
        } catch (IOException e) {
            System.err.printf(String.format("Error: %s", e.getMessage()));
            return false;
        }
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
        injector.getInstance(KeyController.class).init();
        injector.getInstance(TransactionController.class).init();
        injector.getInstance(AccountRecoveryController.class).init();

        return true;
    }

    private static boolean handleArgs(Properties serverProp) {
        String keystorePath;
        String host;
        String portString;
        try {
            keystorePath = IOUtils.getPropertyChecked(serverProp, "keystore");
            host = IOUtils.getPropertyChecked(serverProp, "nodeAddress");
            portString = IOUtils.getPropertyChecked(serverProp, "nodePort");
        } catch (IOException e) {
            System.err.println(String.format("Error: %s", e.getMessage()));
            return false;
        }

        try {
            int port = Integer.parseInt(portString);
            Constants.setNodeAddress(new InetSocketAddress(host, port));
        } catch (NumberFormatException e) {
            System.err.println("Misformatted port number: " + portString);
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
            bind(ConnectionProvider.class).to(ProductionConnectionProvider.class);
            bind(MailService.class).to(GmailService.class);
        }
    }
}
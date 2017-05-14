package server;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import message.payloads.PingPayload;
import server.annotations.DatabasePassword;
import server.annotations.NodeAddress;
import server.config.DatabaseConfig;
import server.controllers.*;
import server.utils.*;
import utils.IOUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Properties;

import static spark.Spark.*;


public class Application {
    private static final int PING_NUMBER = 5431;

    public static boolean run(Properties serverProp) {
        if (!handleArgs(serverProp)) {
            return false;
        }

        int serverPort;
        try {
            serverPort = Integer.parseInt(IOUtils.getPropertyChecked(serverProp, "serverPort"));
        } catch (IOException | NumberFormatException e) {
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
        String keystorePassword;
        try {
            keystorePath = IOUtils.getPropertyChecked(serverProp, "keystore");
            host = IOUtils.getPropertyChecked(serverProp, "nodeAddress");
            portString = IOUtils.getPropertyChecked(serverProp, "nodePort");
            keystorePassword = IOUtils.getPropertyChecked(serverProp, "keystorePassword");
            Module.DB_PASSWORD = IOUtils.getPropertyChecked(serverProp, "databasePassword");
        } catch (IOException e) {
            System.err.println(String.format("Error: %s", e.getMessage()));
            return false;
        }

        try {
            int port = Integer.parseInt(portString);
            Module.NODE_ADDRESS = new InetSocketAddress(host, port);
            if (!pingNode(Module.NODE_ADDRESS)) {
                return false;
            }
        } catch (NumberFormatException e) {
            System.err.println("Misformatted port number: " + portString);
            return false;
        }

        secure(keystorePath, keystorePassword, null, null);
        return true;
    }

    private static boolean pingNode(InetSocketAddress address) {
        try (Socket socket = new Socket(address.getAddress(), address.getPort())) {
            DataOutputStream socketOut = new DataOutputStream(socket.getOutputStream());
            new PingPayload(PING_NUMBER).toMessage().serialize(socketOut);
        } catch (IOException e) {
            System.err.println(String.format("Unable to connect to node at %s", address));
            System.err.println(e.getMessage());
            return false;
        }
        return true;
    }

    private static class Module extends AbstractModule {
        private static String DB_PASSWORD = null;
        private static InetSocketAddress NODE_ADDRESS = null;

        @Override
        protected void configure() {
            bind(ConnectionProvider.class).to(ProductionConnectionProvider.class);
            bind(MailService.class).to(GmailService.class);
            bind(CryptocurrencyEndpoint.Provider.class)
                    .to(ProductionCryptocurrencyEndpoint.Provider.class);

            if (DB_PASSWORD == null) {
                throw new IllegalStateException("DB_PASSWORD not initialized");
            }
            bind(String.class).annotatedWith(DatabasePassword.class).toInstance(DB_PASSWORD);

            if (NODE_ADDRESS == null) {
                throw new IllegalStateException("NODE_ADDRESS not initialized");
            }
            bind(InetSocketAddress.class).annotatedWith(NodeAddress.class)
                    .toInstance(NODE_ADDRESS);
        }
    }
}

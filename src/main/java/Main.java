import cli.ClientInterface;
import crypto.Crypto;
import crypto.ECDSAKeyPair;
import crypto.ECDSAPrivateKey;
import crypto.ECDSAPublicKey;
import network.Miner;
import network.Node;
import server.Application;
import utils.DeserializationException;
import utils.IOUtils;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;

public class Main {

    private static String serverPropertiesPath = "server.properties";
    private static String nodePropertiesPath = "node.properties";

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println(
                    "No command specified.\n\n" +
                    "Usage:\n" +
                    "    <COMMAND> [NODE_PROPERTIES_PATH] [SERVER_PROPERTIES_PATH]\n" +
                    "  COMMANDS:\n" +
                    "    node\n" +
                    "    miner\n" +
                    "    client\n" +
                    "    webserver");
            System.exit(1);
        }
        if (args.length >= 2) {
            nodePropertiesPath = args[1];
        }
        if (args.length >= 3) {
            serverPropertiesPath = args[2];
        }
        Properties nodeProp;
        try (InputStream input = new FileInputStream(nodePropertiesPath)) {
            nodeProp = new Properties();
            nodeProp.load(input);
        } catch (FileNotFoundException e) {
            System.err.println("File \'" + nodePropertiesPath + "\' not found. Aborting...");
            System.exit(1);
            return;
        } catch (IOException e) {
            System.err.println("Unexpected error while reading the node config file. Aborting...");
            System.exit(1);
            return;
        }
        Crypto.init();
        switch (args[0]) {
            case "node":
                if (!runNode(nodeProp)) {
                    System.exit(1);
                }
                break;
            case "miner":
                if (!runMiner(nodeProp)) {
                    System.exit(1);
                }
            case "client":
                new ClientInterface().startInterface();
                break;
            case "webserver":
                Properties serverProp;
                try (InputStream input = new FileInputStream(serverPropertiesPath)) {
                    serverProp = new Properties();
                    serverProp.load(input);
                } catch (FileNotFoundException e) {
                    System.err.println("File \'" + serverPropertiesPath + "\' not found. Aborting...");
                    System.exit(1);
                    return;
                } catch (IOException e) {
                    System.err.println("Unexpected error while reading the server config file. Aborting...");
                    System.exit(1);
                    return;
                }
                if (!Application.run(serverProp, nodeProp)) {
                    System.exit(1);
                }
                if (!runNode(nodeProp)) {
                    System.exit(1);
                }
                break;
            default:
                String msg = String.format("Unrecognized command %s", args[0]);
                System.err.println(msg);
                System.exit(1);
        }
    }

    private static boolean runMiner(Properties prop) {
        return runNode(prop, true);
    }

    private static boolean runNode(Properties prop) {
        return runNode(prop, false);
    }

    private static boolean runNode(Properties prop, boolean isMining) {
        try {
            return runNodeWithThrowing(prop, isMining);
        } catch (IOException e) {
            System.err.println(String.format("Error: %s", e.getMessage()));
            return false;
        }
    }

    private static boolean runNodeWithThrowing(Properties prop, boolean isMining)
            throws IOException {
        int port = Integer.parseInt(IOUtils.getPropertyChecked(prop, "nodePort"));
        ECDSAPublicKey myPublic;
        ECDSAPrivateKey myPrivate;
        ECDSAPublicKey privilegedKey;
        Miner miner = null;
        Node node = null;
        try {
            myPublic = Crypto.loadPublicKey(IOUtils.getPropertyChecked(prop, "publicKey"));
            myPrivate = Crypto.loadPrivateKey(IOUtils.getPropertyChecked(prop, "privateKey"));
            privilegedKey = Crypto.loadPublicKey(IOUtils.getPropertyChecked(prop, "privilegedKey"));
        } catch (DeserializationException e) {
            System.err.println(String.format("Error: %s", e.getMessage()));
            return false;
        }

        if (isMining) {
            miner = new Miner(new ServerSocket(port), new ECDSAKeyPair(myPrivate, myPublic), privilegedKey);
        } else {
            node = new Node(new ServerSocket(port), new ECDSAKeyPair(myPrivate, myPublic), privilegedKey);
        }

        String nodeListPath = IOUtils.getPropertyChecked(prop, "nodeList");
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(nodeListPath), StandardCharsets.UTF_8));
        String currentLine = "";

        try {
            currentLine = br.readLine();
        } catch (IOException e) {
            br.close();
            System.err.println(String.format("Error: %s", e.getMessage()));
        }

        while (currentLine != null) {
            Optional<InetSocketAddress> optAddr = IOUtils.parseAddress(currentLine);
            if (!optAddr.isPresent()) {
                String msg = String.format("Invalid address %s", currentLine);
                System.err.println(msg);
            } else {
                InetSocketAddress addr = optAddr.get();
                if (isMining) {
                    miner.connect(addr.getHostName(), addr.getPort());
                } else {
                    node.connect(addr.getHostName(), addr.getPort());
                }
            }
            try {
                currentLine = br.readLine();
            } catch (IOException e) {
                br.close();
                System.err.println(String.format("Error: %s", e.getMessage()));
                break;
            }
        }
        br.close();
        if (isMining) {
            miner.startMiner();
        } else {
            node.startNode();
        }
        return true;
    }
}

import cli.ClientInterface;
import com.beust.jcommander.JCommander;
import crypto.Crypto;
import crypto.ECDSAKeyPair;
import crypto.ECDSAPrivateKey;
import crypto.ECDSAPublicKey;
import jcommander.CommandClient;
import jcommander.CommandMiner;
import jcommander.CommandNode;
import jcommander.CommandWebserver;
import network.Miner;
import network.Node;
import server.Application;
import utils.DeserializationException;
import utils.IOUtils;
import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;

public class Main {

    public static void main(String[] args) {
        CommandClient cc = new CommandClient();
        CommandMiner cm = new CommandMiner();
        CommandNode cn = new CommandNode();
        CommandWebserver cw = new CommandWebserver();

        JCommander jc = new JCommander();
        jc.setProgramName("yaccoin");
        jc.addCommand("client", cc);
        jc.addCommand("miner", cm);
        jc.addCommand("node", cn);
        jc.addCommand("webserver", cw);

        if (args.length == 0) {
            jc.usage();
            System.exit(0);
        }

        try {
            jc.parse(args);
        } catch (RuntimeException e) {
            jc.usage();
            System.exit(0);
        }

        Crypto.init();

        switch (jc.getParsedCommand()) {
            case "node": {
                Properties nodeProp = parseConfigFile(cn.configFilePath);
                if (nodeProp == null || !runNode(nodeProp)) {
                    System.exit(1);
                }
                break;
            }
            case "miner": {
                Properties nodeProp = parseConfigFile(cm.configFilePath);
                if (nodeProp == null || !runMiner(nodeProp)) {
                    System.exit(1);
                }
            }
            case "client": {
                new ClientInterface().startInterface();
                break;
            }
            case "webserver": {
                Properties serverProp = parseConfigFile(cw.serverConfigFile);
                if (serverProp == null || !Application.run(serverProp)) {
                    System.exit(1);
                }
                if (cw.runNode) {
                    Properties nodeProp = parseConfigFile(cw.nodeConfigFile);
                    if (nodeProp == null || !runNode(nodeProp)) {
                        System.exit(1);
                    }
                }
                break;
            }
            default: {
                // Theoretically unreachable as parsing should fail
                String msg = String.format("Unrecognized command %s", args[0]);
                System.err.println(msg);
                jc.usage();
                System.exit(1);
            }
        }
    }

    private static Properties parseConfigFile(String path) {
        try (InputStream input = new FileInputStream(path)) {
            Properties prop = new Properties();
            prop.load(input);
            return prop;
        } catch (FileNotFoundException e) {
            System.err.println("File \'" + path + "\' not found. Aborting...");
            return null;
        } catch (IOException e) {
            System.err.println("Unexpected error while reading the node config file. Aborting...");
            return null;
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
        Node node;
        try {
            myPublic = Crypto.loadPublicKey(IOUtils.getPropertyChecked(prop, "publicKey"));
            myPrivate = Crypto.loadPrivateKey(IOUtils.getPropertyChecked(prop, "privateKey"));
            privilegedKey = Crypto.loadPublicKey(IOUtils.getPropertyChecked(prop, "privilegedKey"));
        } catch (DeserializationException | IOException e) {
            System.err.println(String.format("Error: %s", e.getMessage()));
            return false;
        }

        if (isMining) {
            node = new Miner(new ServerSocket(port), new ECDSAKeyPair(myPrivate, myPublic), privilegedKey);
        } else {
            node = new Node(new ServerSocket(port), new ECDSAKeyPair(myPrivate, myPublic), privilegedKey);
        }

        String[] nodes = IOUtils.getPropertyChecked(prop, "nodeList").split(",");

        for (String s: nodes) {
            Optional<InetSocketAddress> optAddr = IOUtils.parseAddress(s);
            if (!optAddr.isPresent()) {
                String msg = String.format("Invalid address %s", s);
                System.err.println(msg);
                return false;
            } else {
                InetSocketAddress addr = optAddr.get();
                node.connect(addr.getHostName(), addr.getPort());
            }
        }

        if (node instanceof Miner) {
            ((Miner) node).startMiner();
        } else {
            node.startNode();
        }

        return true;
    }
}

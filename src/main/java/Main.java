import cli.ClientInterface;
import com.beust.jcommander.JCommander;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
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
import utils.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.stream.Collectors;

public class Main {
    private static final Log LOGGER = Log.parentLog();

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

        Injector injector = injector();
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
                injector.getInstance(ClientInterface.class).startInterface();
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
        String logpath;
        Node node;
        try {
            myPublic = Crypto.loadPublicKey(IOUtils.getPropertyChecked(prop, "publicKey"));
            myPrivate = Crypto.loadPrivateKey(IOUtils.getPropertyChecked(prop, "privateKey"));
            privilegedKey = Crypto.loadPublicKey(IOUtils.getPropertyChecked(prop, "privilegedKey"));
            logpath = IOUtils.getPropertyChecked(prop, "logfilePath");
        } catch (DeserializationException | IOException e) {
            System.err.println(String.format("Error: %s", e.getMessage()));
            return false;
        }

        try {
            FileHandler filelog = new FileHandler(logpath);
            LOGGER.logger.addHandler(filelog);
            LOGGER.info("Logging to file %s", logpath);
        } catch (IOException | SecurityException e) {
            LOGGER.warning("Cannot log to file %s.", logpath);
            LOGGER.warning(e.toString());
        }

        if (isMining) {
            node = new Miner(new ServerSocket(port), new ECDSAKeyPair(myPrivate, myPublic), privilegedKey);
        } else {
            node = new Node(new ServerSocket(port), new ECDSAKeyPair(myPrivate, myPublic), privilegedKey);
        }

        String nodeList = IOUtils.getPropertyChecked(prop, "nodeList");
        List<String> nodes = Arrays.stream(nodeList.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

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

    private static Injector injector() {
        return Guice.createInjector(new Module());
    }

    private static final class Module extends AbstractModule {
        @Override
        protected void configure() {
        }
    }

}

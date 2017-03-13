import cli.ClientInterface;
import network.Miner;
import server.Application;
import utils.Crypto;
import utils.IOUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Optional;

public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("No command specified");
            System.exit(1);
        }
        Crypto.init();
        switch (args[0]) {
            case "node":
                if (!runNode(args)) {
                    System.exit(1);
                }
                break;
            case "client":
                new ClientInterface().startInterface();
                break;
            case "webserver":
                Application.run();
            default:
                String msg = String.format("Unrecognized command %s", args[0]);
                System.err.println(msg);
                System.exit(1);
        }
    }

    private static boolean runNode(String[] args) throws GeneralSecurityException, IOException {
        if (args.length < 5) {
            System.err.println("usage: node <port> <public-key> <private-key> <privileged-key> (<ip-address>:<port>)*");
            return false;
        }
        int port = Integer.parseInt(args[1]);
        PublicKey myPublic = Crypto.loadPublicKey(args[2]);
        PrivateKey myPrivate = Crypto.loadPrivateKey(args[3]);
        PublicKey privilegedKey = Crypto.loadPublicKey(args[4]);

        Miner miner = new Miner(port, new KeyPair(myPublic, myPrivate), privilegedKey);
        for (int i = 5; i < args.length; i++) {
            Optional<InetSocketAddress> optAddr = IOUtils.parseAddress(args[i]);
            if (!optAddr.isPresent()) {
                String msg = String.format("Invalid address %s", args[i]);
                System.err.println(msg);
                continue;
            }
            InetSocketAddress addr = optAddr.get();
            miner.connect(addr.getHostName(), addr.getPort());
        }
        miner.startMiner();
        return true;
    }
}

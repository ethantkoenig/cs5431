import cli.ClientInterface;
import network.Miner;
import utils.Crypto;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

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
                ;
                break;
            case "client":
                new ClientInterface().startInterface();
                break;
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
            String[] pieces = args[i].split(":");
            if (pieces.length != 2) {
                String msg = String.format("Invalid address %s", args[i]);
                System.err.println(msg);
                continue;
            }
            miner.connect(pieces[0], Integer.parseInt(pieces[1]));
        }
        miner.startMiner();
        return true;
    }
}

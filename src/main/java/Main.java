import cli.ClientInterface;
import network.Miner;
import server.Application;
import utils.*;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Optional;

public class Main {

    public static void main(String[] args) {
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
                if (!Application.run(args)) {
                    System.exit(1);
                }
                break;
            default:
                String msg = String.format("Unrecognized command %s", args[0]);
                System.err.println(msg);
                System.exit(1);
        }
    }


    private static boolean runNode(String[] args) {
        try {
            return runNodeWithThrowing(args);
        } catch (GeneralSecurityException | IOException e) {
            System.err.println(String.format("Error: %s", e.getMessage()));
            return false;
        }
    }

    private static boolean runNodeWithThrowing(String[] args)
            throws GeneralSecurityException, IOException {
        if (args.length < 6) {
            System.err.println("usage: node <port> <public-key> <private-key> <privileged-key> <File for list of nodes>");
            return false;
        }
        int port = Integer.parseInt(args[1]);
        ECDSAPublicKey myPublic;
        ECDSAPrivateKey myPrivate;
        ECDSAPublicKey privilegedKey;
        try {
            myPublic = Crypto.loadPublicKey(args[2]);
            myPrivate = Crypto.loadPrivateKey(args[3]);
            privilegedKey = Crypto.loadPublicKey(args[4]);
        } catch (DeserializationException e) {
            System.err.println(String.format("Error: %s", e.getMessage()));
            return false;
        }

        Miner miner = new Miner(new ServerSocket(port), new ECDSAKeyPair(myPrivate, myPublic), privilegedKey);

        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(args[5]), StandardCharsets.UTF_8));
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
                miner.connect(addr.getHostName(), addr.getPort());
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
        miner.startMiner();
        return true;
    }
}

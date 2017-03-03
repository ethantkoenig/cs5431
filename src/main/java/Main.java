import cli.GenerateKey;
import cli.Transact;
import network.Node;
import utils.Crypto;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("No argument given");
            System.exit(1);
        }

        Crypto.init();

        switch (args[0]) {
            case "node":
                new Node().accept();
                break;
            case "generate":
                GenerateKey.run(args);
                break;
            case "transact":
                Transact.run(args);
                break;
            default:
                String msg = String.format("Unrecognized command %s", args[0]);
                System.err.println(msg);
                System.exit(1);
        }
    }
}

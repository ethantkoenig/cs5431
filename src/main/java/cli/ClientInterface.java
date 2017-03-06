package cli;
import network.Node;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.PrintStream;
import java.io.IOException;
import java.util.Scanner;
import java.util.HashMap;
import java.util.Map;
import java.io.UnsupportedEncodingException;
import java.lang.IllegalArgumentException;
import java.security.GeneralSecurityException;

/**
 * The ClientInterface object is used to give the user a CLI based on a given
 * input and output stream. These default to std.in and std.out when not
 * explicitly declared.
 */
public class ClientInterface {

    /**
     * Command interface is used for parsing/running commands that the user
     * gives to the client.
     */
    interface Command {
        /**
         * Takes in a Scanner which can parse the argument, and perfrom the
         * corresponding actions. Returns a boolean to signal if the actions
         * was successful or not.
         *
         * @param args Scanner which contains the arguments from the user
         */
        boolean run(Scanner args);

        /**
         * Takes in a PrintStream, prints documentation of the command
         *
         * @param out PrintStream to display documentation to
         */
        void documentation(PrintStream out);
    }

    // Reader which reads the input stream reader, buffers for efficiency
    private BufferedReader buffer;
    private InputStreamReader streamReader;
    private InputStream streamIn;
    protected PrintStream outputStream;
    private String nodeListPath;

    // A HashMap off all the commands
    protected Map<String, Command> commands = new HashMap<>();

    /**
     * Create a new client interface which by default reads from System.in as
     * UTF8 encoding and outputs to System.out.
     */
    public ClientInterface() {
        this(System.in, System.out, "UTF8");
    }

    /**
     * Creates a new client interface with a custom input stream.
     *
     * @param in InputStream to recieve data from user
     * @param out PrintStream to send data to the user
     * @param encoding Charset which the input will be encoded as
     */
    public ClientInterface(InputStream in, PrintStream out, String encoding) {
        streamIn = in;
        try {
        streamReader = new InputStreamReader(streamIn, encoding);
        } catch (UnsupportedEncodingException e){
            throw new IllegalArgumentException("Not a valid encoding");
        }
        buffer = new BufferedReader(streamReader);
        outputStream = out;
        nodeListPath = "";
        populateCmdMap();
    }

    /**
     * Starts the client interface with the user. This will not return until
     * the user indicates that they want to quit.
     */
    public void startInterface() {
        outputStream.println("Welcome!");
        outputStream.println(streamReader.getEncoding());
        String cmd = "";
        // If previous command was quit no action was taken and we will now exit
        while (!cmd.equals("quit")) {
            try {
                // Make sure that buffered stream is ready
                while (!buffer.ready()) {}
                // Get command from buffer
                cmd = buffer.readLine();
            } catch (IOException e) {
                outputStream.println("Something went wrong");
            }
            // Make sure we did not read a null value
            if (cmd != null) {
                Scanner cmdScanner = new Scanner(cmd);
                cmd = cmdScanner.next();
                if (cmd != null && commands.containsKey(cmd)) {
                    commands.get(cmd).run(cmdScanner);
                } else {
                    outputStream.println("Invalid command");
                }
            // In case cmd is null, set to empty string as we want to noop
            } else {
                cmd = "";
            }
        }
    }

    /**
     * Adds all of the commands to the HashMap of commands
     */
    private void populateCmdMap() {
        commands.put("help", help());
        commands.put("quit", quit());
        commands.put("node", node());
        commands.put("generate", generate());
        commands.put("transact", transact());
        commands.put("setNodelist", setNodeList());
    }

    /**************************************************************************
     * Command anonymous functions are implemented below here                             *
     **************************************************************************/

    /**
     * helper function for "node" command. Sets up a node on the machine
     */
    private Command node() {
        return new Command() {

            /**
             * Creates a Node object and allows it to start accepting incoming
             * connections and processing data
             *
             * @param args this command takes in no additional arguments
             */
            @Override
            public boolean run(Scanner args) {
                int port = args.nextInt();
                try {
                    new Node(port).accept();
                } catch(IOException e) {
                    e.printStackTrace();
                }
                return true;
            }

            @Override
            public void documentation(PrintStream out) {
                out.println("node - Launch a ledger-keeping, block-mining node.");
                out.println("This node will use a generated private/public key pair.");
            }
        };
    }

    /**
     * helper function for the "generate" command. Generate a public/private key
     */
    private Command generate() {
        return new Command() {

            /**
             * Generates a public/private key and writes to specified files
             *
             * @param args command takes 2 arguments, the public key filename
             * and then the private key filename
             */
            @Override
            public boolean run(Scanner args) {
                String publicFid;
                String privateFid;
                if (args.hasNext()) {
                    publicFid = args.next();
                } else {
                    outputStream.println("Not enough arguments");
                    return false;
                }
                if (args.hasNext()) {
                    privateFid = args.next();
                } else {
                    outputStream.println("Not enough arguments");
                    return false;
                }
                try {
                GenerateKey.generateKey(privateFid, publicFid);
                } catch(GeneralSecurityException | IOException e) {
                    e.printStackTrace();
                }
                return true;
            }

            @Override
            public void documentation(PrintStream out) {
                out.print("generate <public filename> <private filename> - ");
                out.print("Generates a public/private key pair, and writes");
                out.println(" key to specified file");
            }
        };
    }

    /**
     * helper function for the "transact" command which then gives additional
     * prompts for creating transactions. It uses the current list of nodes
     * to publish transactions to
     */
    private Command transact() {
        return new Command() {

            /**
             * Creates and publishes transactions to nodes in nodeListpath
             *
             * @param args This command takes no arguments
             */
            @Override
            public boolean run(Scanner args) {
                try {
                    Transact.run(nodeListPath);
                } catch(IOException e) {
                    e.printStackTrace();
                }
                return true;
            }

            @Override
            public void documentation(PrintStream out) {
                out.print("transact - make a transaction, the user will be");
                out.println(" prompted for inputs");
            }
        };
    }

    /**
     * Helper function for the "setNodeList" command which specifies the path of
     * the file which has the list of nodes used for broadcasting transactions
     */
    private Command setNodeList() {
        return new Command() {

            /**
             * Sets the path for where the list of nodes is
             *
             * @param args String of filepath to list of nodes
             */
            @Override
            public boolean run(Scanner args) {
                if (args.hasNext()) {
                    nodeListPath = args.next();
                } else {
                    outputStream.println("Not enough arguments");
                    return false;
                }
                return true;
            }

            @Override
            public void documentation(PrintStream out) {
                out.print("setNodeList <filename> - Sets the file which ");
                out.println("lists the nodes to broadcast transactions to.");
                out.println("Default location is <enter later>");
                out.println("Each line is formatted: <IP address> <port #>");
            }
        };
    }

    /**
     * help function is for the "help" command. This outputs the documentation
     * for each command to the output stream.
     */
    private Command help() {
        return new Command() {
            /**
             * Outputs the documentation for all commands
             *
             * @ param args Scanner that contains any arguments for the command
             *    This command takes no arguments
             */
            @Override
            public boolean run(Scanner args) {
                for (String s : commands.keySet()) {
                    commands.get(s).documentation(outputStream);
                }
                return true;
            }

            @Override
            public void documentation(PrintStream out) {
                out.println("help - displays documentation for all commands");
            }
        };
    }

    /**
     * Quit exits the user from the CLI
     */
    private Command quit() {
        return new Command() {
            /**
             * Perform a noop. The main while loop will exit.
             *
             * @param args Scanner that contains any arguments for the command
             *    This command takes no arguments
             */
            @Override
            public boolean run(Scanner args) {
                return true;
            }

            @Override
            public void documentation(PrintStream out) {
                out.println("quit - exits the program");
            }
        };
    }
}

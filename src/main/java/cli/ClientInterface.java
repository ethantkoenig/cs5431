package cli;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

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
         * Takes in a Scanner which can parse the argument, and perform the
         * corresponding actions.
         *
         * @param args Scanner which contains the arguments from the user
         * @return whether the action was successful
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
    protected PrintStream outputStream;
    private String nodeListPath = null;
    private boolean quit = false;

    // A HashMap off all the commands
    protected Map<String, Command> commands = new HashMap<>();

    /**
     * Create a new client interface which by default reads from System.in as
     * UTF8 encoding and outputs to System.out.
     */
    public ClientInterface() {
        this(new InputStreamReader(System.in, StandardCharsets.UTF_8), System.out);
    }

    /**
     * Creates a new client interface with a custom input stream.
     *
     * @param in  InputStream to recieve data from user
     * @param out PrintStream to send data to the user
     */
    public ClientInterface(Reader in, PrintStream out) {
        buffer = new BufferedReader(in);
        outputStream = out;
        populateCmdMap();
    }

    /**
     * Starts the client interface with the user. This will not return until
     * the user indicates that they want to quit.
     */
    public void startInterface() {
        outputStream.println("Welcome!");

        String inputLine = null; // line read from input
        while (!quit) {
            try {
                outputStream.print("> ");
                inputLine = buffer.readLine();
            } catch (IOException e) {
                outputStream.println("Something went wrong");
            }

            if (inputLine != null) {
                Scanner cmdScanner = new Scanner(inputLine);
                if (!cmdScanner.hasNext()) {
                    continue;
                }
                String cmd = cmdScanner.next();
                if (commands.containsKey(cmd)) {
                    commands.get(cmd).run(cmdScanner);
                } else {
                    String msg = String.format(
                            "Unrecognized command %s; use the \"help\" command to list all commands",
                            cmd);
                    outputStream.println(msg);
                }
            }
        }
    }

    /**
     * Adds all of the commands to the HashMap of commands
     */
    private void populateCmdMap() {
        commands.put("help", help());
        commands.put("quit", quit());
        commands.put("generate", generate());
        commands.put("transact", transact());
        commands.put("setNodeList", setNodeList());
    }

    /* *************************************************************************
     * Command anonymous functions are implemented below here                  *
     * *************************************************************************/

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
                    outputStream.println("usage: generate <public-filename> <private-filename>");
                    return false;
                }
                if (args.hasNext()) {
                    privateFid = args.next();
                } else {
                    outputStream.println("usage: generate <public-filename> <private-filename>");
                    return false;
                }
                try {
                    GenerateKey.generateKey(privateFid, publicFid);
                } catch (GeneralSecurityException | IOException e) {
                    e.printStackTrace();
                    return false;
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
                if (nodeListPath == null) {
                    outputStream.println("No nodes have been specified");
                    return false;
                }
                try {
                    Transact.run(buffer, nodeListPath);
                } catch (GeneralSecurityException | IOException e) {
                    e.printStackTrace();
                    return false;
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
             * Quit the interactive shell.
             *
             * @param args Scanner that contains any arguments for the command
             *    This command takes no arguments
             */
            @Override
            public boolean run(Scanner args) {
                quit = true;
                return true;
            }

            @Override
            public void documentation(PrintStream out) {
                out.println("quit - exits the program");
            }
        };
    }
}

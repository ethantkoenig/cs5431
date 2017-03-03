package cli;

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

/*
 * The ClinetInterface object is used to give the user a CLI based on a given
 * input and output stream. These default to std.in and std.out when not
 * explicitly declared.
 */
public class ClientInterface {

    /*
     * Command interface is used for parsing/running commands that the user
     * gives to the client.
     */
    interface Command {
        /*
         * Takes in a Scanner which can parse the arguement, and perfrom the
         * corresponding actions. Returns a boolean to signal if the actions
         * was successful or not.
         *
         * @param args Scanner which contains the arguements from the user
         */
        boolean run(Scanner args);

        /*
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

    // A HashMap off all the commands
    protected Map<String, Command> commands = new HashMap<>();

    /*
     * Create a new client interface which by default reads from System.in as
     * UTF8 encoding and outputs to System.out.
     */
    public ClientInterface() {
        this(System.in, System.out, "UTF8");
    }

    /*
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
        populateCmdMap();
    }

    /*
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

    /*
     * Adds all of the commands to the HashMap of commands
     */
    private void populateCmdMap() {
        commands.put("help", help());
        commands.put("quit", quit());
    }

    /**************************************************************************
     * Command anonymous functions are implemented below here                             *
     **************************************************************************/

    /*
     * help function is for the "help" command. This outputs the documentation
     * for each command to the output stream.
     */
    private Command help() {
        return new Command() {
            /*
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

            /*
             * Takes in a PrintStream, prints documentation of the command
             *
             * @param out PrintStream to display documentation to
             */
            @Override
            public void documentation(PrintStream out) {
                out.println("help - displays documentation for all commands");
            }
        };
    }

    /*
     * Quit exits the user from the CLI
     */
    private Command quit() {
        return new Command() {
            /*
             * Perform a noop. The main while loop will exit.
             *
             * @param args Scanner that contains any arguements for the command
             *    This command takes no arguements
             */
            @Override
            public boolean run(Scanner args) {
                return true;
            }

            /*
             * Takes in a PrintStream, prints documentation of the command
             *
             * @param out PrintStream to display documentation to
             */
            @Override
            public void documentation(PrintStream out) {
                out.println("quit - exits the program");
            }
        };
    }
}

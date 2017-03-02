import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.PrintStream;
import java.io.IOException;
import java.util.Scanner;
import java.util.HashMap;
import java.util.Map;

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
         */
        boolean run(Scanner args);

        /*
         * Takes in a PrintStream, prints documentation of the command
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
     * Create a new client interface which by default reads from System.in and
     * outputs to System.out.
     */
    public ClientInterface() {
        this(System.in, System.out);
    }

    /*
     * Creates a new client interface with a custom input stream.
     *
     * @param in InputStream to recieve data from user
     * @param out PrintStream to send data to the user
     */
    public ClientInterface(InputStream in, PrintStream out) {
        streamIn = in;
        streamReader = new InputStreamReader(streamIn);
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
            Scanner cmdScanner = new Scanner(cmd);
            try {
                // run given command, if invalid let user know
                commands.get(cmdScanner.next()).run(cmdScanner);
            } catch (NullPointerException e) {
                outputStream.println("Invalid command");
            }
        }
    }

    /*
     * Adds all of the commands to the HashMap of commands
     */
    private void populateCmdMap() {
        commands.put("help", new HelpMenu());
        commands.put("quit", new Quit());
    }

    /**************************************************************************
     * Command classes are implemented below here                             *
     **************************************************************************/

    /*
     * HelpMenu class is for the "help" command. This outputs the documentation
     * for each command to the output stream.
     */
    class HelpMenu implements Command {
        /*
         * Outputs the documentation for all commands
         */
        public boolean run(Scanner args) {
            for (String s : commands.keySet()) {
                commands.get(s).documentation(outputStream);
            }
            return true;
        }

        public void documentation(PrintStream out) {
            out.println("help - displays documentation for all commands");
        }
    }

    /*
     * Quit exits the user from the CLI
     */
    class Quit implements Command {
        /*
         * Perform a noop. The main while loop will exit.
         */
        public boolean run(Scanner args) {
            return true;
        }

        public void documentation(PrintStream out) {
            out.println("quit - exits the program");
        }
    }
}

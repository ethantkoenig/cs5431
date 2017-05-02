package jcommander;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(separators = "=", commandDescription = "Run the server for the web app")
public class CommandWebserver {

    @Parameter(
            names = { "-c", "--config" },
            description = "The path to the server configuration file to use")
    public String serverConfigFile = "server.properties";

    @Parameter(
            names = { "-n", "--node" },
            description = "Run a node along side the server")
    public boolean runNode = false;

    @Parameter(
            names = "--node-config",
            description = "The path to the node configuration file to use (if spawning a node)")
    public String nodeConfigFile = "node.properties";
}

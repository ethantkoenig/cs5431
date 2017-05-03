package jcommander;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandDescription = "Run a non mining node in the network")
public class CommandNode {

    @Parameter(
            names = { "-c", "--config" },
            required = true,
            description = "The path to the configuration file to use")
    public String configFilePath = null;
}

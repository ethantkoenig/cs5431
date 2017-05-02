package jcommander;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(separators = "=", commandDescription = "Run a mining node in the network")
public class CommandMiner {

        @Parameter(
                names = { "-c", "--config" },
                description = "The path to the configuration file to use")
        public String configFilePath = "node.properties";
}

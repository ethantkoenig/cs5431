package jcommander;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import edu.umd.cs.findbugs.annotations.SuppressWarnings;

@SuppressWarnings(
        value="UWF_NULL_FIELD",
        justification="Field is written by JCommander, which is opaque to findbugs"
)
@Parameters(commandDescription = "Run a mining node in the network")
public class CommandMiner {

        @Parameter(
                names = { "-c", "--config" },
                required = true,
                description = "The path to the configuration file to use")
        public String configFilePath = null;
}

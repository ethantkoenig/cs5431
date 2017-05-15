package jcommander;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandDescription = "Run the server for the web app")
public class CommandWebserver {

    @Parameter(
            names = {"-sc", "--server-config"},
            description = "The path to the server configuration file to use")
    public String serverConfigFile = "server.properties";
}

package cli;

import org.junit.Assert;
import org.junit.Test;
import utils.Crypto;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ClientInterfaceTest {

    @Test
    public void testHelpQuit() {
        String output = runCli("help", "", " ", "quit");
        Assert.assertTrue(output.contains("help"));
        Assert.assertTrue(output.contains("quit"));
        Assert.assertTrue(output.contains("generate"));
        Assert.assertTrue(output.contains("transact"));
        Assert.assertTrue(output.contains("setNodeList"));
    }

    @Test
    public void testUnrecognized() {
        runCli("notacommand", "quit");
    }

    @Test
    public void testGenerate() throws Exception {
        String publicPath = File.createTempFile("public", ".tmp").getAbsolutePath();
        String privatePath = File.createTempFile("public", ".tmp").getAbsolutePath();
        runCli(
                "generate",
                String.format("generate %s", publicPath),
                String.format("generate %s %s", publicPath, privatePath),
                "quit"
        );
        Crypto.loadPublicKey(publicPath);
        Crypto.loadPrivateKey(privatePath);
    }

    private String runCli(String... inputs) {
        BufferedReader in = new BufferedReader(
                new StringReader(String.join("\n", inputs))
        );
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            PrintStream out = new PrintStream(outputStream, true, StandardCharsets.UTF_8.displayName());

            ClientInterface cli = new ClientInterface(in, out);
            cli.startInterface();
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        } catch (UnsupportedEncodingException e) {
            Assert.fail(e.getMessage());
            throw new AssertionError("Cannot reach here");
        }
    }
}

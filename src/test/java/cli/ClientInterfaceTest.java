package cli;

import network.ConnectionThread;
import network.Message;
import org.junit.Assert;
import org.junit.Test;
import testutils.RandomizedTest;
import testutils.TestUtils;
import transaction.Transaction;
import utils.Crypto;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ClientInterfaceTest extends RandomizedTest {

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
        String privatePath = File.createTempFile("private", ".tmp").getAbsolutePath();
        runCli(
                "generate",
                String.format("generate %s", publicPath),
                String.format("generate %s %s", publicPath, privatePath),
                "quit"
        );
        Crypto.loadPublicKey(publicPath);
        Crypto.loadPrivateKey(privatePath);
    }

    @Test
    public void testTransact() throws Exception {
        File temp = File.createTempFile("nodes", ".tmp");
        TestUtils.writeFile(temp.getAbsolutePath(), "localhost:12345");

        BlockingQueue<Message> queue = new ArrayBlockingQueue<Message>(5);

        ServerSocket serverSocket = new ServerSocket(12345);
        new Thread(() -> {
            try {
                Socket socket = serverSocket.accept();
                new ConnectionThread(socket, queue).start();
            } catch (IOException e) {
                queue.add(null); // signal that Thread had error
            }
        }).start();

        String publicPath = File.createTempFile("public", ".tmp").getAbsolutePath();
        String privatePath = File.createTempFile("private", ".tmp").getAbsolutePath();
        runCli(
                String.format("setNodeList %s", temp.getAbsolutePath()),
                String.format("generate %s %s", publicPath, privatePath),
                "transact", // start transact
                "notANumber!", // first attempt fails
                "transact", // retry
                "1", // 1 input
                randomShaTwoFiftySix().toString(),
                "0",
                privatePath,
                "1", // 1 output
                publicPath,
                "500",
                "quit"
        );
        Message m = queue.take();
        Assert.assertNotNull(errorMessage, m);
        Assert.assertEquals(errorMessage, Message.TRANSACTION, m.type);

        ByteBuffer buffer = ByteBuffer.wrap(m.payload);
        Transaction transaction = Transaction.deserialize(buffer);
        Assert.assertEquals(errorMessage, 1, transaction.numInputs);
        Assert.assertEquals(errorMessage, 1, transaction.numOutputs);
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

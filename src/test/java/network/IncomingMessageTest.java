package network;

import org.junit.Assert;
import org.junit.Test;
import testutils.RandomizedTest;
import testutils.TestUtils;
import utils.DeserializationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class IncomingMessageTest extends RandomizedTest {

    private static final IncomingMessage.MessageResponder emptyResponder = r -> {
    };

    @Test
    public void testEqualsHashCode() {
        byte[] payload1 = randomBytes(random.nextInt(1024));
        byte[] payload2 = randomBytes(1024);
        IncomingMessage m1 = new IncomingMessage(
                Message.TRANSACTION, payload1, emptyResponder);
        IncomingMessage m2 = new IncomingMessage(
                Message.TRANSACTION, payload1, emptyResponder);
        IncomingMessage m3 = new IncomingMessage(
                Message.TRANSACTION, payload2, emptyResponder);

        TestUtils.assertEqualsWithHashCode(errorMessage, m1, m1);
        TestUtils.assertEqualsWithHashCode(errorMessage, m1, m2);

        assertNotEquals(errorMessage, m1, m3);
        assertNotEquals(errorMessage, m1, null);
    }

    @Test
    public void testRespond() throws Exception {
        List<OutgoingMessage> responses = new ArrayList<>();
        byte[] payload = randomBytes(random.nextInt(1024));
        IncomingMessage m = new IncomingMessage(
                Message.TRANSACTION, payload, responses::add);

        OutgoingMessage m1 = new OutgoingMessage(Message.BLOCK, randomBytes(1024));
        m.respond(m1);
        OutgoingMessage m2 = new OutgoingMessage(Message.GET_BLOCK, randomBytes(1024));
        m.respond(m2);
        assertEquals(errorMessage, Arrays.asList(m1, m2), responses);
    }

    @Test
    public void testDeserialize() throws Exception {
        byte[] payload = randomBytes(random.nextInt(1024));
        IncomingMessage m = new IncomingMessage(
                Message.TRANSACTION, payload, emptyResponder);

        OutgoingMessage out = new OutgoingMessage(m.type, m.payload);

        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        out.serialize(new DataOutputStream(byteOutput));

        ByteArrayInputStream byteInput = new ByteArrayInputStream(byteOutput.toByteArray());
        IncomingMessage deserialized = IncomingMessage.deserializer(emptyResponder)
                .deserialize(new DataInputStream(byteInput));
        TestUtils.assertEqualsWithHashCode(errorMessage, m, deserialized);
    }

    @Test(expected = DeserializationException.class)
    public void testDeserializeFailure() throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        dataOutputStream.writeInt(Integer.MAX_VALUE);
        dataOutputStream.write(randomBytes(1024));

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                byteArrayOutputStream.toByteArray());
        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);

        IncomingMessage.responderlessDeserializer().deserialize(dataInputStream);
        Assert.fail("Should have thrown");
    }
}

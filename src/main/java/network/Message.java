package network;

import utils.ByteUtil;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.net.InetAddress;

/**
 * Represents a message sent or received on the network
 *
 * @version 1.0, Feb 28 2017
 */
public class Message {
    /* Message kinds */
    public static final byte TRANSACTION = 0;
    public static final byte BLOCK = 1;

    public InetAddress hostIP;
    public int hostPort;
    public final byte type;
    public final byte[] payload;

    public Message(byte type, byte[] payload) {
        this.type = type;
        this.payload = Arrays.copyOf(payload, payload.length);
    }

    /**
     * Sets host IP for the message.
     * XXX: Might want to move this to the constructor and make the field final.
     * @param ip is the users IP
     */
    public void setHostIP(String ip) throws UnknownHostException {
        hostIP = InetAddress.getByName(ip);
    }

    /**
     *
     * @param port is the port the user will receive a reply from.
     * XXX: Might want to move this to the constructor and make the field final.
     */
    public void setHostPort(int port) {
        hostPort = port;
    }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", payload=" + ByteUtil.bytesToHexString(payload) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message message = (Message) o;

        if (type != message.type) return false;
        return Arrays.equals(payload, message.payload);

    }

    @Override
    public int hashCode() {
        int result = (int) type;
        result = 31 * result + Arrays.hashCode(payload);
        return result;
    }
}

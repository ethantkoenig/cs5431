package server.utils;

import java.net.InetSocketAddress;

public class Constants {
    /**
     * Address of cryptocurrency node to connect to for transactions
     */
    private static InetSocketAddress nodeAddress;

    public static InetSocketAddress getNodeAddress() {
        if (nodeAddress == null) {
            throw new IllegalStateException("Cannot access unpopulated field");
        }
        return nodeAddress;
    }

    public static void setNodeAddress(InetSocketAddress address) {
        if (nodeAddress != null) {
            throw new IllegalStateException("Cannot re-set populated field");
        }
        nodeAddress = address;
    }
}

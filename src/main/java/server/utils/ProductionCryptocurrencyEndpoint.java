package server.utils;

import com.google.inject.Inject;
import message.IncomingMessage;
import message.OutgoingMessage;
import server.annotations.NodeAddress;
import utils.DeserializationException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ProductionCryptocurrencyEndpoint implements CryptocurrencyEndpoint {
    private final Socket socket;
    private final DataOutputStream outputStream;
    private final DataInputStream inputStream;

    public ProductionCryptocurrencyEndpoint(InetSocketAddress address) throws IOException {
        socket = new Socket(address.getAddress(), address.getPort());
        outputStream = new DataOutputStream(socket.getOutputStream());
        inputStream = new DataInputStream(socket.getInputStream());
    }

    @Override
    public void send(OutgoingMessage message) throws IOException {
        message.serialize(outputStream);
    }

    @Override
    public IncomingMessage receive() throws DeserializationException, IOException {
        return IncomingMessage.deserializer(this::send).deserialize(inputStream);
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
        inputStream.close();
        socket.close();
    }

    public static final class Provider implements CryptocurrencyEndpoint.Provider {
        private final InetSocketAddress address;

        @Inject
        public Provider(@NodeAddress InetSocketAddress address) {
            this.address = address;
        }

        @Override
        public CryptocurrencyEndpoint getEndpoint() throws IOException {
            return new ProductionCryptocurrencyEndpoint(address);
        }
    }
}

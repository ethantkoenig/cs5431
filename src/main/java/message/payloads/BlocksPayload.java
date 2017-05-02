package message.payloads;

import block.Block;
import message.Message;
import utils.CanBeSerialized;
import utils.DeserializationException;
import utils.Deserializer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class BlocksPayload extends MessagePayload {
    public static final Deserializer<BlocksPayload> DESERIALIZER =
            new BlocksPayloadDeserializer();
    private final List<Block> blocks;

    public BlocksPayload(List<Block> blocks) {
        this.blocks = blocks;
    }

    @Override
    public byte messageType() {
        return Message.BLOCKS;
    }

    @Override
    public void serialize(DataOutputStream outputStream) throws IOException {
        CanBeSerialized.serializeList(outputStream, blocks);
    }

    private static final class BlocksPayloadDeserializer implements Deserializer<BlocksPayload> {
        @Override
        public BlocksPayload deserialize(DataInputStream inputStream)
                throws DeserializationException, IOException {
            return new BlocksPayload(Deserializer.deserializeList(inputStream, Block.DESERIALIZER));
        }
    }


}

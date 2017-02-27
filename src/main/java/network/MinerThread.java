package network;

import block.Block;
import utils.ByteUtil;
import utils.ShaTwoFiftySix;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * The MinerThread is run as a background thread by a Node and is responsible for mining.
 * This entails constantly looking for the correct nonce in order to generate a sufficiently small
 * hash value.
 *
 * @author Evan King
 * @version 1.0, Feb 22 2017
 */
public class MinerThread extends Thread{

    private ShaTwoFiftySix hashGoal;
    private Block block;

    public MinerThread(ShaTwoFiftySix hashGoal, Block block) {
        this.hashGoal = hashGoal;
        this.block = block;
    }

    public void setHashGoal(byte[] hashGoal) throws GeneralSecurityException {
        this.hashGoal = ShaTwoFiftySix.hashOf(hashGoal);
    }

    public void setBlock(Block block){
        this.block = block;
    }

    private static ShaTwoFiftySix computeHash(Block block, byte[] nonce) throws IOException {
        ShaTwoFiftySix hash = null;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        block.serialize(os);
        try {
            byte[] bytes = ByteUtil.concatenate(os.toByteArray(), nonce);
            hash = ShaTwoFiftySix.hashOf(bytes);
        } catch (GeneralSecurityException e) {
            //TODO: Logging
            e.printStackTrace();
        }
        return hash;
    }

    private boolean checkHash(ShaTwoFiftySix hash){
        return hash.compareTo(this.hashGoal) <= 0;
    }

    private byte[] tryNonces(Block block) throws IOException {
        byte[] nonce = new byte[100000000];
        while (nonce.toString().compareTo("100000000") < 0){

            ShaTwoFiftySix hash = computeHash(block, nonce);
            if (checkHash(hash))
                return nonce;

            nonce = ByteUtil.addOne(nonce);
        }
        return null;
    }

    @Override
    public void run() {

    }


}

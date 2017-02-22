import utils.Crypto;

import java.nio.charset.Charset;
import java.security.KeyPair;

public class Main {
    public static void main(String[] args) throws Exception {
        Crypto.init();

        byte[] toSign = "Hey there, sign me".getBytes(Charset.defaultCharset());
        KeyPair pair = Crypto.signatureKeyPair();
        byte[] signed = Crypto.sign(toSign, pair.getPrivate());
        if (Crypto.verify(toSign, signed, pair.getPublic())) {
            System.out.println("It worked!");
        } else {
            System.out.println("Signature failed");
            System.exit(1);
        }
    }
}

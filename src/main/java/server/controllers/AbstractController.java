package server.controllers;

import java.math.BigInteger;
import java.security.SecureRandom;

public abstract class AbstractController {

    public abstract void init();

    final String nextGUID(SecureRandom random) {
        return new BigInteger(130, random).toString(32);
    }

}

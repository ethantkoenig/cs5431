package utils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Various system-wide constants and configurations. These "constants" should be
 * modified only in tests.
 */
public class Config {

    private final static int DEFAULT_BCRYPT_COST = 12;
    public final static AtomicInteger BCRYPT_COST =
            new AtomicInteger(DEFAULT_BCRYPT_COST);

    private final static int DEFAULT_HASH_GOAL = 2;
    public final static AtomicInteger HASH_GOAL =
            new AtomicInteger(DEFAULT_HASH_GOAL);

}

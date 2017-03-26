package utils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Various system-wide constants and configurations. These "constants" should be
 * modified only in tests.
 */
public class Config {

    private final static int DEFAULT_PBKDF2_COST = 12;
    public final static AtomicInteger PBKDF2_COST =
            new AtomicInteger(DEFAULT_PBKDF2_COST);

    private final static int DEFAULT_HASH_GOAL = 2;
    public final static AtomicInteger HASH_GOAL =
            new AtomicInteger(DEFAULT_HASH_GOAL);

}

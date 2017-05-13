package utils;

/**
 * Various system-wide constants and configurations. These "constants" should be
 * modified only in tests.
 */
public final class Config {

    private static int PBKDF2_COST = 12;
    private static int HASH_GOAL = 2;

    // Disallow instances of this class
    private Config() {
    }

    public static int pbkdf2Cost() {
        return PBKDF2_COST;
    }

    public static void setPbkdf2Cost(int cost) {
        PBKDF2_COST = cost;
    }

    public static int hashGoal() {
        return HASH_GOAL;
    }

    public static void setHashGoal(int goal) {
        HASH_GOAL = goal;
    }
}

package utils;

public class Longs {

    /**
     * @return whether adding {@code a} and {@code b} results in arithmetic
     * overflow.
     */
    public static boolean sumWillOverflow(long a, long b) {
        long sum = a + b;
        if (a < 0 && b < 0 && sum >= 0) {
            return true;
        } else if (a >= 0 && b >= 0 && sum < 0) {
            return true;
        }
        return false;
    }
}

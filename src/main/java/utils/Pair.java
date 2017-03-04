package utils;

import java.util.Arrays;

/**
 * Created by eperdew on 3/3/17.
 */
public class Pair<L, R> {
    private final L left;
    private final R right;

    /**
     * Creates a new {@code Pair} object containing {@code left} and {@code right}.
     *
     * @param left The first item to be stored in the {@code Pair}.
     * @param right The second item to be stored in the {@code Pair}.
     */
    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    /**
     * @return The first object in this {@code Pair}
     */
    public L getLeft() {
        return left;
    }

    /**
     * @return The second object in this {@code Pair}
     */
    public R getRight() {
        return right;
    }

    @Override public boolean equals(Object other) {
        if (other instanceof Pair) {
            Pair<L,R> o = (Pair) other;
            return o.left.equals(left) && o.right.equals(right);
        }
        return false;
    }

    @Override public int hashCode() {
        return Arrays.hashCode(new Object[]{ left, right });
    }
}

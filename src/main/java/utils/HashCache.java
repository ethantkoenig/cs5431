package utils;

import java.util.Optional;

/**
 * Created by eperdew on 3/26/17.
 */
public abstract class HashCache {

    private Optional<ShaTwoFiftySix> val = Optional.empty();

    /**
     * @return The SHA-256 hash of `this`
     */
    public ShaTwoFiftySix getShaTwoFiftySix() {
        return val.orElseGet(() -> {
            val = Optional.of(computeHash());
            return val.get();
        });
    }

    /**
     * Compute the hash of `this`
     */
    protected abstract ShaTwoFiftySix computeHash();

    /**
     * Invalidate the cached SHA-256 hash.
     *
     * Call this after any function that mutates `this` in a way that changes the hash.
     */
    public void invalidateCache() {
        val = Optional.empty();
    }
}

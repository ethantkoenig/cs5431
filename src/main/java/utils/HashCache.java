package utils;

/**
 * Created by eperdew on 3/26/17.
 */
public interface HashCache {

    /**
     * @return The SHA-256 hash of `this`
     */
    ShaTwoFiftySix getShaTwoFiftySix();

    /**
     * Invalidate the cached SHA-256 hash.
     *
     * Call this after any function that mutates `this` in a way that changes the hash.
     */
    void invalidateCache();
}

package transaction;

import utils.ShaTwoFiftySix;

import java.util.Optional;

/**
 * An interface for looking up transactions by hash
 */
@FunctionalInterface
public interface TransactionLookup {
    // boolean contains(ShaTwoFiftySix hash);
    Optional<RTransaction> lookup(ShaTwoFiftySix hash);
}

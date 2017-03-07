package transaction;

import utils.ShaTwoFiftySix;

import java.util.Optional;

/**
 * An interface for looking up transactions by hash
 */
@FunctionalInterface
public interface TransactionLookup {
    // boolean contains(ShaTwoFiftySix hash);
    Optional<Transaction> lookup(ShaTwoFiftySix hash);
}

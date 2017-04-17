package utils;

import java.util.Optional;
import java.util.stream.Stream;

public final class Optionals {
    private Optionals() {
        // don't instantiate
    }

    public static <T> Stream<T> stream(Optional<T> optional) {
        return optional.map(Stream::of).orElse(Stream.empty());
    }
}

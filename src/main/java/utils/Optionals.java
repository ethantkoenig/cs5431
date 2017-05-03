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

    public static <T, E extends Exception> void ifPresent(Optional<T> optional,
                                                          ThrowingConsumer<T, E> consumer) throws E {
        if (!optional.isPresent()) {
            return;
        }
        consumer.consume(optional.get());
    }

    @FunctionalInterface
    public interface ThrowingConsumer<T, E extends Exception> {
        void consume(T value) throws E;
    }
}

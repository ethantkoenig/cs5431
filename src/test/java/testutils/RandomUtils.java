package testutils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class RandomUtils {
    private RandomUtils() {
        // disallow instances
    }

    public static <T> T choiceIterable(Random random, Iterable<T> iterable) {
        List<T> list = new ArrayList<>();
        iterable.forEach(list::add);
        return choiceList(random, list);
    }

    public static <T> T choiceList(Random random, List<T> list) {
        return list.get(random.nextInt(list.size()));
    }
}

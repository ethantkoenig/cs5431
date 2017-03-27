package testutils;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import java.util.ArrayList;

public class Generators {
    public static <E> ArrayList<E> generateList(
            int size, Generator<E> gen, SourceOfRandomness rand, GenerationStatus status) {
        ArrayList<E> result = new ArrayList<>();
        for (int i = 0; i < size; ++i) {
            result.add(gen.generate(rand, status));
        }
        return result;
    }
}

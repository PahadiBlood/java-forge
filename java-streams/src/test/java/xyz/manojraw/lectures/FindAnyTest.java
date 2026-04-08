package xyz.manojraw.lectures;


import java.util.Arrays;
import java.util.function.Predicate;

import org.junit.Test;

public class FindAnyTest {

    final Predicate<Integer> numbersLessThan10 = n -> n > 5 && n < 10;

    @Test
    public void findAny() throws Exception {
        Integer[] numbers = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        System.out.println(
                Arrays.stream(numbers)
                        .filter(numbersLessThan10)
                        .findAny()
                        .get()
        );
    }

    @Test
    public void findFirst() throws Exception {
        Integer[] numbers = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        System.out.println(Arrays.stream(numbers)
                .filter(numbersLessThan10)
                .findFirst()
                .get()
        );
    }
}


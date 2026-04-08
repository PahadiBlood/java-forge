package xyz.manojraw.lectures;


import com.google.common.collect.ImmutableList;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;


import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MinMaxTest {

    Comparator<Integer> sortNumber = (o1,o2) -> o1-o2;

    @Test
    public void min() throws Exception {
        final List<Integer> numbers = ImmutableList.of(1, 2, 3, 100, 23, 93, 99);
        System.out.println("Min number from list "+numbers);
        Optional<Integer> res = numbers.stream().min(sortNumber);
        res.ifPresent(System.out::println);

        //test
        assertThat(res.get()).isEqualTo(1);
    }

    @Test
    public void max() throws Exception {
        final List<Integer> numbers = ImmutableList.of(1, 2, 3, 100, 23, 93, 99);
        Optional<Integer> res = numbers.stream().max(sortNumber);
        System.out.println("Max number from list "+numbers);
        res.ifPresent(System.out::println);

        assertThat(res.get()).isEqualTo(100);
    }
}

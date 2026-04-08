package xyz.manojraw.lectures;

import com.google.common.collect.ImmutableList;

import java.util.*;
import java.util.stream.Collectors;

import org.junit.Test;

public class DistinctTest {

    @Test
    public void distinct() throws Exception {
        final List<Integer> numbers = ImmutableList.of(1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 9, 9, 9);
        numbers.stream().distinct().forEach(System.out::println);
    }

    @Test
    public void distinctWithSet() throws Exception {
        final List<Integer> numbers = ImmutableList.of(1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 9, 9, 9);
        Set<Integer> distinctNumbers = new HashSet<>(numbers);
        distinctNumbers.forEach(System.out::println);

        numbers.stream().collect(Collectors.toSet()).forEach(System.out::println);
    }
}

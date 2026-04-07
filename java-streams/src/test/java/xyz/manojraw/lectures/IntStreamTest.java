package xyz.manojraw.lectures;

import xyz.manojraw.beans.Person;

import java.util.List;
import java.util.stream.IntStream;

import xyz.manojraw.mockdata.MockData;
import org.junit.Test;

public class IntStreamTest {

    @Test
    public void range() throws Exception {

        // Imperative way
        for (int i = 0; i < 10; i++) {
            System.out.println(i);
        }

        // Declarative way
        IntStream.range(0, 10).forEach(System.out::println);

        System.out.println("Inclusive");
        IntStream.rangeClosed(0, 10).forEach(System.out::println);
    }

    @Test
    public void rangeIteratingLists() throws Exception {
        List<Person> people = MockData.getPeople();
        IntStream
                .range(0, people.size())
                .forEach(index -> System.out.println(people.get(index)));
    }

    @Test
    public void intStreamIterate() throws Exception {
        IntStream.
                iterate(1, operand -> operand + 1)
                .limit(20)
                .filter(v -> v % 2 == 0)
                .forEach(System.out::println);
    }
}

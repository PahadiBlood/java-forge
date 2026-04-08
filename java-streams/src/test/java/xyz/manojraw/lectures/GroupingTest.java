package xyz.manojraw.lectures;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.assertj.core.util.Lists;
import org.junit.Test;
import xyz.manojraw.beans.Car;
import xyz.manojraw.mockdata.MockData;

public class GroupingTest {

    @Test
    public void simpleGrouping() throws Exception {
        MockData.getCars().stream()
                .collect(Collectors.groupingBy(Car::getMake));

        Map<String, Long> res = MockData.getCars().stream().collect(Collectors.groupingBy(Car::getMake, Collectors.counting()));

        res.forEach((key, value) -> System.out.println("Key: " + key + " value: " + value));
    }

    @Test
    public void groupingAndCounting() throws Exception {
        ArrayList<String> names = Lists
                .newArrayList(
                        "John",
                        "John",
                        "Mariam",
                        "Alex",
                        "Mohammado",
                        "Mohammado",
                        "Vincent",
                        "Alex",
                        "Alex"
                );

        names.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .forEach((k, v) -> System.out.println(k + " : " + v));
    }

}
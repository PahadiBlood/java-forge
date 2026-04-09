package xyz.manojraw.lectures;


import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;

public class FlatMapTest {

    private static final List<ArrayList<String>> arrayListOfNames = Lists.newArrayList(
            Lists.newArrayList("Mariam", "Alex", "Ismail"),
            Lists.newArrayList("John", "Alesha", "Andre"),
            Lists.newArrayList("Susy", "Ali")
    );

    @Before
    public void setUp() {
        System.out.println(arrayListOfNames);
    }

    @Test
    public void withoutFlatMap() throws Exception {
//    [Mariam, Alex, Ismail, John, Alesha, Andre, Susy, Ali]
        System.out.println("Without Flat Map");
        for (ArrayList<String> wrapper : arrayListOfNames) {
            for (String name : wrapper) {
                System.out.println("Name: " + name);
            }
        }
    }

    @Test
    public void withFlatMap() throws Exception {
//   [Mariam, Alex, Ismail, John, Alesha, Andre, Susy, Ali]

        //Flatmap combines inner collection to a single collection

        System.out.println();
        System.out.println("With Flat Map->");
        arrayListOfNames.stream().flatMap(
                Collection::stream).collect(Collectors.toList()).forEach(System.out::println);

    }

}


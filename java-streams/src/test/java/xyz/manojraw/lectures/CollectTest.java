package xyz.manojraw.lectures;

import xyz.manojraw.beans.Person;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import xyz.manojraw.mockdata.MockData;
import org.junit.Test;

public class CollectTest {
    @Test
    public void understandingCollect() throws Exception {
        List<String> emails = MockData.getPeople()
                .stream()
                .map(Person::getEmail)
                .collect(ArrayList::new,
                        ArrayList::add,
                        ArrayList::addAll
                );

        emails.forEach(System.out::println);
    }
}

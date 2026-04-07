package xyz.manojraw.lectures;

import com.google.common.collect.Lists;
import xyz.manojraw.beans.Person;

import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import xyz.manojraw.mockdata.MockData;
import org.junit.Test;


public class ImperativeDeclarativeApproachTest {

    @Test
    public void imperativeApproach() throws IOException {
        int limit = 10;
        int count = 0;

        List<Person> people = MockData.getPeople();
        // 1. Find people aged less or equal 18
        // 2. Then change implementation to find first 10 people

        List<Person> youngPeople = Lists.newArrayList();
        for (Person p : people) {
            if (p.getAge() <= 18) {
                youngPeople.add(p);
                count++;

                if (count == limit) break;
            }
        }

        for (Person young : youngPeople) {
            System.out.println(young);
        }
    }

    @Test
    public void declarativeApproachUsingStreams() throws Exception {
        ImmutableList<Person> people = MockData.getPeople();

        List<Person> youngPeoples = people
                .stream()
                .filter(p -> p.getAge() <= 18)
                .limit(10)
                .collect(Collectors.toList());

        youngPeoples.forEach(System.out::println);
    }
}

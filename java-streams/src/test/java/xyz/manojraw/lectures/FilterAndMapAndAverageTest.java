package xyz.manojraw.lectures;


import static org.assertj.core.api.Assertions.assertThat;

import xyz.manojraw.beans.Car;
import xyz.manojraw.beans.Person;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.stream.Collectors;

import xyz.manojraw.beans.PersonDTO;
import xyz.manojraw.mockdata.MockData;
import org.junit.Test;

public class FilterAndMapAndAverageTest {

    @Test
    public void understandingFilter() throws Exception {
        ImmutableList<Car> cars = MockData.getCars();
        cars.stream()
                .filter(c -> c.getPrice() < 10000)
                .filter(c -> "Toyota".equalsIgnoreCase(c.getMake()))
                .forEach(System.out::println);
    }

    @Test
    public void ourFirstMapping() throws Exception {
        // transform from one data type to another
        List<Person> people = MockData.getPeople();
        List<PersonDTO> persons = people.stream()
                .map(PersonDTO::map)
                .collect(Collectors.toList());

        persons.forEach(System.out::println);
        assertThat(persons).hasSize(1000);
    }

    @Test
    public void averageCarPrice() throws Exception {
        // calculate average of car prices
        List<Car> cars = MockData.getCars();

        Double averageCarPrice = cars.stream()
                .mapToDouble(Car::getPrice)
                .average()
                .orElse(0);
        System.out.println(Math.round(averageCarPrice));
    }
}




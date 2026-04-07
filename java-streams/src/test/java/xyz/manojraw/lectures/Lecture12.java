package xyz.manojraw.lectures;

import xyz.manojraw.beans.Person;

import java.util.List;
import java.util.stream.Collectors;
import xyz.manojraw.mockdata.MockData;
import org.junit.Test;

public class Lecture12 {
  @Test
  public void understandingCollect() throws Exception {
    List<String> emails = MockData.getPeople()
        .stream()
        .map(Person::getEmail)
        .collect(Collectors.toList());

    emails.forEach(System.out::println);
  }
}

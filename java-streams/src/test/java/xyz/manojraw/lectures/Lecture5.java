package xyz.manojraw.lectures;


import static org.assertj.core.api.Assertions.assertThat;

import xyz.manojraw.beans.Car;
import xyz.manojraw.beans.Person;
import com.google.common.collect.ImmutableList;
import java.util.List;

import xyz.manojraw.mockdata.MockData;
import org.junit.Test;

public class Lecture5 {

  @Test
  public void understandingFilter() throws Exception {
    ImmutableList<Car> cars = MockData.getCars();

  }

  @Test
  public void ourFirstMapping() throws Exception {
    // transform from one data type to another
    List<Person> people = MockData.getPeople();

  }

  @Test
  public void averageCarPrice() throws Exception {
    // calculate average of car prices

  }

  @Test
  public void test() throws Exception {

  }
}




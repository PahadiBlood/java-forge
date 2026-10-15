package xyz.manojraw.exercises.interview.dto;

import xyz.manojraw.exercises.interview.modal.Car;
import xyz.manojraw.exercises.interview.modal.People;

import java.util.List;

public record CarsAndPeople(List<Car> cars, List<People> people) {
}
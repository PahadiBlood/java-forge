package xyz.manojraw.exercises.interview.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import xyz.manojraw.exercises.interview.modal.Car;
import xyz.manojraw.exercises.interview.modal.People;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public enum FileService {
    INSTANCE;

    private static final String CAR_PATH = "/Users/mac/Documents/Github/java-forge/java-core/src/test/resources/cars.json";
    private static final String PEOPLE_PATH = "/Users/mac/Documents/Github/java-forge/java-core/src/test/resources/people.json";

    public List<Car> readAndGetCars() throws IOException {
        try (FileReader f = new FileReader(CAR_PATH)) {
            return new Gson().fromJson(f, new TypeToken<List<Car>>() {
            }.getType());
        }
    }

    public List<People> readAndGetPeople() throws IOException {
        try (FileReader f = new FileReader(PEOPLE_PATH)) {
            return new Gson().fromJson(f, new TypeToken<List<People>>() {
            }.getType());
        }
    }
}

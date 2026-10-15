package xyz.manojraw.exercises.interview.tasks.completablefuture;

import xyz.manojraw.exercises.interview.modal.Car;
import xyz.manojraw.exercises.interview.modal.People;
import xyz.manojraw.exercises.interview.service.FileService;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Reads cars.json and people.json.
 * <p>
 * readCars() and readPeople() run on the caller thread.
 * The async methods start that same work on the executor you pass in.
 * <p>
 * Pass the executor from outside. This class does not create a pool
 * and does not shut one down.
 * <p>
 * CompletableFutureTest shows why these methods look this way.
 */
public enum DataService {
    INSTANCE;


    private final static FileService FILE_SERVICE = FileService.INSTANCE;

    public List<Car> readCars() throws IOException {
        var cars = FILE_SERVICE.readAndGetCars();
        return cars != null ? cars : List.of();
    }

    public List<People> readPeople() throws IOException {
        var people = FILE_SERVICE.readAndGetPeople();
        return people != null ? people : List.of();
    }

    public CompletableFuture<List<Car>> readCarsAsync(Executor executor) {
        return CompletableFuture.supplyAsync(() -> wrap(this::readCars), executor);
    }

    public CompletableFuture<List<People>> readPeopleAsync(Executor executor) {
        return CompletableFuture.supplyAsync(() -> wrap(this::readPeople), executor);
    }

    @FunctionalInterface
    private interface IoCall<T> {
        T get() throws IOException;
    }

    private static <T> T wrap(IoCall<T> call) {
        try {
            return call.get();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

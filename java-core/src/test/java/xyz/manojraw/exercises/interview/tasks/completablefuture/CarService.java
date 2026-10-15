package xyz.manojraw.exercises.interview.tasks.completablefuture;

import xyz.manojraw.exercises.interview.dto.CarRequest;
import xyz.manojraw.exercises.interview.dto.CarsAndPeople;
import xyz.manojraw.exercises.interview.dto.PaginationWrapper;
import xyz.manojraw.exercises.interview.modal.Car;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class CarService {

    private final static DataService DATA_SERVICE = DataService.INSTANCE;
    private final ExecutorService service = Executors.newFixedThreadPool(5);

    //private static volatile int id;
    private static final AtomicInteger ID = new AtomicInteger();
    private static final List<Car> cars;

    static {
        try {
            cars = new CopyOnWriteArrayList<>(DATA_SERVICE.readCars());
            ID.set(!cars.isEmpty() ? cars.getLast().id() : 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Task 1: findAll()
//   Return all cars (from FileService or an in-memory list).
    public List<Car> findAll() {
        return cars;
    }

    // Task 2: findById(int id)
//   Return a single car. Use stream().filter().findFirst() and return Optional<Car>.
    public Car findById(int id) {
        return cars
                .stream()
                .filter(c -> c.id() == id)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Car not found"));
    }
// Task 3: create(Car car)
//   Add a new car to the list. Validate that the ID doesn't already exist.

    public Car create(CarRequest request) {
        var isCarExist = cars
                .stream()
                .anyMatch(c -> isMatch(c, request));

        if (isCarExist) {
            throw new RuntimeException("Car already exists");
        }

        int id = generateCarId();
        return saveCars(request, id);
    }

    private Car saveCars(CarRequest request, int id) {
        Car car = new Car(
                id,
                request.make(),
                request.model(),
                request.price(),
                request.year(),
                request.color()
        );
        cars.add(car);
        return car;
    }

    private int generateCarId() {
        return ID.incrementAndGet();
    }

    private boolean isMatch(Car car, CarRequest request) {
        return
                Objects.equals(car.model(), request.model()) &&
                        Objects.equals(car.make(), request.make()) &&
                        Objects.equals(car.color(), request.color()) &&
                        Objects.equals(car.price(), request.price()) &&
                        Objects.equals(car.year(), request.year());
    }

// Task 4: update(int id, Car updatedCar)
//   Find the car by ID and replace its fields. Handle the "not found" case.

//    public Car update(int id, CarUpdateRequest request) {
//        if (id < 1) {
//            throw new IllegalArgumentException("Invalid Id");
//        }
//
//        var existingCar = cars
//                .stream()
//                .filter(c -> c.id() == id)
//                .findFirst().orElseThrow(() -> new NoSuchElementException(
//                        String.format("Car with id: %s not found", id)
//                ));
//
//        // needs to change car record to car object
//        return existingCar;
//    }

    // Task 5: delete(int id)
//   Remove the car by ID. Return true/false depending on whether it existed.
    public void deleteById(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Invalid Id");
        }

        var car = cars
                .stream()
                .filter(c -> c.id() == id)
                .findFirst()
                .orElse(null);

        if (car != null) {
            cars.remove(car);
        }
    }

    // Task 6: search(String model, double minPrice, double maxPrice)
//   Filter using streams. Make the filters optional (skip a filter if the param is null).
    public List<Car> search(String make, BigDecimal minPrice, BigDecimal maxPrice) {

//        var searchChain = cars.stream();
//
//        if (make != null && !make.isEmpty()) {
//            searchChain=  searchChain.filter(c -> c.make().equalsIgnoreCase(make));
//        }
//
//        if (minPrice != null && (minPrice.compareTo(BigDecimal.ZERO) >= 0)) {
//            searchChain = searchChain.filter(c -> c.price().compareTo(minPrice) >= 0);
//        }
//
//        if (maxPrice != null && (maxPrice.compareTo(BigDecimal.ZERO) >= 0)) {
//            searchChain = searchChain.filter(c -> c.price().compareTo(maxPrice) <= 0);
//        }
//
//        return searchChain.toList();

        return cars.stream()
                .filter(c -> make == null || c.make().equalsIgnoreCase(make))
                .filter(c -> minPrice == null || c.price().compareTo(minPrice) >= 0)
                .filter(c -> maxPrice == null || c.price().compareTo(maxPrice) <= 0)
                .toList();
    }

// Task 7: findAllPaginated(int page, int size)
//   Return only that page's slice using skip((page - 1) * size) and limit(size).
//   Bonus: also return the total count and total pages.

    public PaginationWrapper<Car> findAllPaginated(int page, int size) {
        if (page <= 0) throw new IllegalArgumentException("Invalid page number");
        if (size <= 0) throw new IllegalArgumentException("Invalid size value");

        long totalItems = cars.size();

        int totalPages = (int) Math.ceil((double) totalItems / size);
        var carData = cars
                .stream()
                .skip((long) (page - 1) * size)
                .limit(size)
                .toList();

        return new PaginationWrapper<>(carData, page, size, totalPages, totalItems);
    }

// Task 8 (async): getAllCarsAsync() and getAllPeopleAsync()
//   Wrap both in CompletableFuture.supplyAsync(..., executor).
//   Pass the ExecutorService in as a parameter, don't create it inside the service.

    public CarsAndPeople getCarAndPeople() {
        try {
            var carAsync = CompletableFuture.supplyAsync(() -> {
                try {
                    return DATA_SERVICE.readCars();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, service);
            var peopleAsync = CompletableFuture.supplyAsync(() -> {
                try {
                    return DATA_SERVICE.readPeople();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, service);

            return carAsync
                    .thenCombine(peopleAsync, CarsAndPeople::new)
                    .join();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            service.shutdown();
        }
    }
// Task 9 (merge): getCarsWithOwners()
//   Use thenCombine to merge the cars future and the people future.
//   Match each car to its owner (or each person to the cars they like).
//   Print the result with thenAccept; don't call get() in the middle.

// Task 10 (errors): findByIdAsync(int id)
//   Throw an exception if the car doesn't exist.
//   Handle it with exceptionally() or handle() and return a fallback value.

// Remember: shut down the executor at the end, and keep main alive with join()
// if you're using callbacks.
}

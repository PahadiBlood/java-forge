package xyz.manojraw.exercises.interview.tasks.completablefuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import xyz.manojraw.exercises.interview.dto.CarRequest;
import xyz.manojraw.exercises.interview.dto.CarsAndPeople;
import xyz.manojraw.exercises.interview.dto.PaginationWrapper;
import xyz.manojraw.exercises.interview.modal.Car;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompletableFutureTaskTest {

    private CarService carService;

    @BeforeEach
    void setUp() {
        carService = new CarService();
    }

    @ParameterizedTest(name = "id {0} is {1} {2}")
    @MethodSource("carIdArguments")
    void findCarById(int id, String make, String model, BigDecimal price, int year, String color) {
        Car car = carService.findById(id);

        assertEquals(new Car(id, make, model, price, year, color), car);
    }

    @ParameterizedTest(name = "id {0} is missing")
    @ValueSource(ints = {0, -1, Integer.MAX_VALUE})
    void findCarByIdWhenMissing(int id) {
        NoSuchElementException error = assertThrows(
                NoSuchElementException.class,
                () -> carService.findById(id)
        );

        assertEquals("Car not found", error.getMessage());
    }

    @ParameterizedTest(name = "{0} {1} {3}")
    @MethodSource("createArguments")
    void createCar(String make, String model, BigDecimal price, int year, String color) {
        int before = carService.findAll().size();
        CarRequest request = new CarRequest(make, model, price, year, color);

        Car created = carService.create(request);

        assertEquals(new Car(created.id(), make, model, price, year, color), created);
        assertEquals(created, carService.findById(created.id()));
        assertEquals(before + 1, carService.findAll().size());

        RuntimeException duplicate = assertThrows(RuntimeException.class, () -> carService.create(request));
        assertEquals("Car already exists", duplicate.getMessage());
        assertEquals(before + 1, carService.findAll().size());
    }

    @ParameterizedTest(name = "{5}")
    @MethodSource("invalidCreateArguments")
    void createCarWhenRequestIsInvalid(
            String make,
            String model,
            BigDecimal price,
            int year,
            String color,
            String message
    ) {
        int before = carService.findAll().size();

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> carService.create(new CarRequest(make, model, price, year, color))
        );

        assertEquals(message, error.getMessage());
        assertEquals(before, carService.findAll().size());
    }

    @ParameterizedTest(name = "parallel cars {0} to {1}")
    @MethodSource("parallelCreateArguments")
    void createCarsInParallel(int from, int to) {
        int before = carService.findAll().size();
        int count = to - from + 1;

        List<Car> created = IntStream.rangeClosed(from, to)
                .parallel()
                .mapToObj(i -> carService.create(parallelRequest(i)))
                .toList();

        assertEquals(count, created.size());
        assertEquals(count, created.stream().map(Car::id).distinct().count());
        assertEquals(before + count, carService.findAll().size());

        for (int i = from; i <= to; i++) {
            Car car = created.get(i - from);
            CarRequest request = parallelRequest(i);
            assertEquals(new Car(car.id(), request.make(), request.model(), request.price(), request.year(), request.color()), car);
            assertEquals(car, carService.findById(car.id()));
        }
    }

    @ParameterizedTest(name = "make={0}, min={1}, max={2}")
    @MethodSource("searchArguments")
    void search(String make, BigDecimal minPrice, BigDecimal maxPrice) throws Exception {
        List<Car> found = carService.search(make, minPrice, maxPrice);
        List<Car> savedMatches = DataService.INSTANCE.readCars()
                .stream()
                .filter(car -> matches(car, make, minPrice, maxPrice))
                .toList();

        assertTrue(found.stream().allMatch(car -> matches(car, make, minPrice, maxPrice)));
        assertTrue(found.containsAll(savedMatches));
    }

    @ParameterizedTest(name = "page {0}, size {1}")
    @MethodSource("pageArguments")
    void findAllPaginated(int page, int size) {
        List<Car> all = carService.findAll();
        int totalPages = (int) Math.ceil((double) all.size() / size);
        List<Car> expected = all.stream()
                .skip((long) (page - 1) * size)
                .limit(size)
                .toList();

        PaginationWrapper<Car> pageResult = carService.findAllPaginated(page, size);

        assertEquals(new PaginationWrapper<>(expected, page, size, totalPages, all.size()), pageResult);
    }

    @ParameterizedTest(name = "page {0}, size {1}")
    @MethodSource("invalidPageArguments")
    void findAllPaginatedWhenArgumentsAreInvalid(int page, int size, String message) {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> carService.findAllPaginated(page, size)
        );

        assertEquals(message, error.getMessage());
    }

    @Test
    void getCarAndPeople() throws Exception {
        CarsAndPeople result = carService.getCarAndPeople(Executors.newFixedThreadPool(5));

        assertEquals(DataService.INSTANCE.readCars(), result.cars());
        assertEquals(DataService.INSTANCE.readPeople(), result.people());
    }

    private static Stream<Arguments> carIdArguments() {
        return Stream.of(
                Arguments.of(1, "Toyota", "Highlander", new BigDecimal("23736.62"), 2008, "Mauv"),
                Arguments.of(2, "Toyota", "Solara", new BigDecimal("66403.47"), 2002, "Fuscia"),
                Arguments.of(3, "Ford", "Expedition EL", new BigDecimal("68973.04"), 2010, "Pink"),
                Arguments.of(5, "Lexus", "SC", new BigDecimal("20900.33"), 2010, "Pink")
        );
    }

    private static Stream<Arguments> createArguments() {
        return Stream.of(
                Arguments.of("Interview-Toyota", "Interview-Highlander", new BigDecimal("12000.50"), 2020, "Red"),
                Arguments.of("Interview-Ford", "Interview-Focus", new BigDecimal("0"), 1769, "Blue"),
                Arguments.of("Interview-Lexus", "Interview-SC", new BigDecimal("20900.33"), 2010, "Pink")
        );
    }

    private static Stream<Arguments> invalidCreateArguments() {
        return Stream.of(
                Arguments.of(null, "Model", new BigDecimal("1000"), 2020, "Red", "Invalid value for Make"),
                Arguments.of("  ", "Model", new BigDecimal("1000"), 2020, "Red", "Invalid value for Make"),
                Arguments.of("Make", null, new BigDecimal("1000"), 2020, "Red", "Invalid value for Model"),
                Arguments.of("Make", "Model", null, 2020, "Red", "Invalid value for Price"),
                Arguments.of("Make", "Model", new BigDecimal("-1"), 2020, "Red", "Invalid value for Price"),
                Arguments.of("Make", "Model", new BigDecimal("1000"), 1768, "Red", "Invalid value for Year"),
                Arguments.of("Make", "Model", new BigDecimal("1000"), 3000, "Red", "Invalid value for Year"),
                Arguments.of("Make", "Model", new BigDecimal("1000"), 2020, null, "Invalid value for Color"),
                Arguments.of("Make", "Model", new BigDecimal("1000"), 2020, " ", "Invalid value for Color")
        );
    }

    private static Stream<Arguments> parallelCreateArguments() {
        return Stream.of(
                Arguments.of(1, 20),
                Arguments.of(21, 40)
        );
    }

    private static Stream<Arguments> searchArguments() {
        return Stream.of(
                Arguments.of("Toyota", new BigDecimal("23736"), new BigDecimal("31696")),
                Arguments.of("toyota", new BigDecimal("23736.62"), new BigDecimal("23736.62")),
                Arguments.of("FORD", null, null),
                Arguments.of(null, new BigDecimal("20000"), new BigDecimal("30000")),
                Arguments.of(null, null, new BigDecimal("25000")),
                Arguments.of(null, new BigDecimal("60000"), null),
                Arguments.of(null, null, null),
                Arguments.of("", null, null),
                Arguments.of("NoSuchMake", null, null),
                Arguments.of("Toyota", new BigDecimal("80000"), new BigDecimal("10000"))
        );
    }

    private static Stream<Arguments> pageArguments() {
        return Stream.of(
                Arguments.of(1, 10),
                Arguments.of(4, 2),
                Arguments.of(2, 25),
                Arguments.of(1_000, 10)
        );
    }

    private static Stream<Arguments> invalidPageArguments() {
        return Stream.of(
                Arguments.of(0, 10, "Invalid page number"),
                Arguments.of(-1, 2, "Invalid page number"),
                Arguments.of(1, 0, "Invalid size value"),
                Arguments.of(3, -5, "Invalid size value")
        );
    }

    private static CarRequest parallelRequest(int i) {
        return new CarRequest(
                "Parallel-" + i,
                "Model-" + i,
                BigDecimal.valueOf(10_000L + i),
                1990 + (i % 30),
                "Color-" + i
        );
    }

    private static boolean matches(Car car, String make, BigDecimal minPrice, BigDecimal maxPrice) {
        if (make != null && !car.make().equalsIgnoreCase(make)) {
            return false;
        }
        if (minPrice != null && car.price().compareTo(minPrice) < 0) {
            return false;
        }
        return maxPrice == null || car.price().compareTo(maxPrice) <= 0;
    }
}

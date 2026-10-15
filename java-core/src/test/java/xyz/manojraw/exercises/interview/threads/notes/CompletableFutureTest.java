package xyz.manojraw.exercises.interview.threads.notes;

import xyz.manojraw.exercises.interview.modal.Car;
import xyz.manojraw.exercises.interview.modal.People;
import xyz.manojraw.exercises.interview.tasks.completablefuture.DataService;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Each class below is one step from the first commented draft.
 * The next class fixes that step and leaves a new gap open.
 *
 * 1. SequentialFileReads reads cars, then people, on main.
 * 2. CallableThenGet starts both on a pool, then get() parks main for each one.
 * 3. InvertedPeoplePoll puts get() inside while (!isDone()).
 * 4. CompletableFutureGetStillBlocks uses supplyAsync, then get() anyway.
 * 5. ForkJoinPoolDaemonNeedsWait uses the common pool. Those threads are daemons.
 * 6. SleepToHoldMain sleeps a guessed number of milliseconds.
 * 7. OwnExecutorThenAccept passes your pool, thenAccept, join, shutdown.
 * 8. CombineCarsAndPeople merges both reads with thenCombine. No get() in the middle.
 * 9. HandleReadError turns a failed read into a fallback with exceptionally().
 *
 * OwnExecutorThenAccept plus CombineCarsAndPeople is the shape to keep.
 * FileReadService holds that code.
 *
 * The write-up is 04_completable_future.md.
 */
public class CompletableFutureTest {
    private final static DataService DATA_SERVICE = DataService.INSTANCE;

    public static void main(String[] args) throws Exception {
        SequentialFileReads.demo();
        CallableThenGet.demo();
        InvertedPeoplePoll.demo();
        CompletableFutureGetStillBlocks.demo();
        ForkJoinPoolDaemonNeedsWait.demo();
        SleepToHoldMain.demo();
        OwnExecutorThenAccept.demo();
        //CombineCarsAndPeople.demo();
        HandleReadError.demo();
        System.out.println();
        System.out.println("each CompletableFuture demo behaved as 04_completable_future.md describes");
    }

    /**
     * Both files are independent. Main still reads cars first, then people.
     * "Other work" prints last.
     */
    static final class SequentialFileReads {

        static void demo() throws IOException {
            header("1. SequentialFileReads — two independent reads wait in a line");
            
            long start = System.currentTimeMillis();
            List<Car> cars = slowCars(DATA_SERVICE);
            System.out.println("cars " + cars.size());
            List<People> people = slowPeople(DATA_SERVICE);
            System.out.println("people " + people.size());
            System.out.println("other work on main");
            System.out.println("elapsed ms " + (System.currentTimeMillis() - start));
        }
    }

    /**
     * Callable + Future lets both reads run at the same time.
     * cars.get() still parks main. people.get() parks main again.
     * A failure inside call() becomes ExecutionException on get().
     */
    static final class CallableThenGet {

        static void demo() {
            header("2. CallableThenGet — overlap on the pool, wait on get()");
            
            ExecutorService pool = Executors.newFixedThreadPool(2);
            long start = System.currentTimeMillis();
            try {
                Callable<List<Car>> futureCars = () -> slowCars(DATA_SERVICE);
                Callable<List<People>> futurePeople = () -> slowPeople(DATA_SERVICE);
                Future<List<Car>> cars = pool.submit(futureCars);
                Future<List<People>> people = pool.submit(futurePeople);
                System.out.println("both reads submitted");
                System.out.println("cars " + cars.get().size());
                System.out.println("people " + people.get().size());
                System.out.println("other work on main");
                System.out.println("elapsed ms " + (System.currentTimeMillis() - start));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                System.out.println("task failed: " + e.getCause());
            } finally {
                shutdown(pool);
            }
        }
    }

    /**
     * The first people poll was:
     *
     *   while (!people.isDone()) {
     *       System.out.println("Total people data " + people.get().size());
     *   }
     *
     * The draft also submitted people only after cars had finished, so the
     * two reads did not overlap. get() inside that while still waits.
     * If people were already done, isDone() is true and the body never runs.
     */
    static final class InvertedPeoplePoll {

        static void demo() throws Exception {
            header("3. InvertedPeoplePoll — get() inside while (!isDone())");
            
            ExecutorService pool = Executors.newFixedThreadPool(2);
            try {
                Future<List<Car>> cars = pool.submit(() -> slowCars(DATA_SERVICE));
                while (true) {
                    if (cars.isDone()) {
                        System.out.println("cars " + cars.get().size());
                        break;
                    }
                }

                Future<List<People>> people = pool.submit(() -> slowPeople(DATA_SERVICE));
                long start = System.currentTimeMillis();
                int prints = 0;
                while (!people.isDone()) {
                    System.out.println("Total people data " + people.get().size());
                    prints++;
                }
                System.out.println("people prints from that loop: " + prints);
                System.out.println("that get() waited ms " + (System.currentTimeMillis() - start));
            } finally {
                shutdown(pool);
            }
        }
    }

    /**
     * supplyAsync starts the read. get() still parks main.
     * The callback style is the reason to pick CompletableFuture.
     * get() gives that up.
     */
    static final class CompletableFutureGetStillBlocks {

        static void demo() {
            header("4. CompletableFutureGetStillBlocks — get() on a completable future");
            
            ExecutorService pool = Executors.newFixedThreadPool(2);
            long start = System.currentTimeMillis();
            try {
                CompletableFuture<List<Car>> cars = DATA_SERVICE.readCarsAsync(pool)
                        .thenApply(list -> {
                            pause(300);
                            return list;
                        });
                CompletableFuture<List<People>> people = DATA_SERVICE.readPeopleAsync(pool)
                        .thenApply(list -> {
                            pause(300);
                            return list;
                        });
                System.out.println("both reads started");
                System.out.println("cars " + cars.get().size());
                System.out.println("people " + people.get().size());
                System.out.println("other work on main");
                System.out.println("elapsed ms " + (System.currentTimeMillis() - start));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                System.out.println("task failed: " + e.getCause());
            } finally {
                shutdown(pool);
            }
        }
    }

    /**
     * supplyAsync with no executor uses ForkJoinPool.commonPool().
     * Those workers are daemon threads. Main can finish while the read
     * is still running. The JVM then exits and the thenAccept body is lost.
     *
     * This demo keeps the process up with join() so you can see the flag flip.
     * Drop that join() and the print after it never happens.
     */
    static final class ForkJoinPoolDaemonNeedsWait {

        static void demo() {
            header("5. ForkJoinPoolDaemonNeedsWait — common pool threads are daemons");
            
            AtomicBoolean printed = new AtomicBoolean(false);
            CompletableFuture<List<Car>> cars = CompletableFuture.supplyAsync(() -> {
                pause(400);
                try {
                    return DATA_SERVICE.readCars();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            CompletableFuture<Void> accept = cars.thenAccept(list -> {
                System.out.println("thenAccept cars " + list.size());
                printed.set(true);
            });
            System.out.println("main reached the next line, printed=" + printed.get());
            accept.join();
            System.out.println("after join, printed=" + printed.get());
        }
    }

    /**
     * Thread.sleep(6000) in the first draft held main so daemon work could finish.
     * The number is a guess. A faster machine wastes time. A slower machine
     * still exits early.
     */
    static final class SleepToHoldMain {

        static void demo() throws InterruptedException {
            header("6. SleepToHoldMain — a guessed sleep to keep main alive");
            
            AtomicBoolean printed = new AtomicBoolean(false);
            CompletableFuture<List<Car>> cars = CompletableFuture.supplyAsync(() -> {
                pause(200);
                try {
                    return DATA_SERVICE.readCars();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            cars.thenAccept(list -> printed.set(true));
            Thread.sleep(600);
            System.out.println("after a guessed sleep, printed=" + printed.get());
            cars.join();
        }
    }

    /**
     * Pass your own pool. thenAccept runs when the list is ready.
     * Main prints "other work" at once. join() waits at the end.
     * shutdown() runs in finally. Pool threads are normal threads,
     * so the JVM stays up until you shut the pool down.
     */
    static final class OwnExecutorThenAccept {

        static void demo() {
            header("7. OwnExecutorThenAccept — your pool, thenAccept, join, shutdown");
            
            ExecutorService pool = Executors.newFixedThreadPool(2);
            try {
                CompletableFuture<List<Car>> cars = DATA_SERVICE.readCarsAsync(pool)
                        .thenApply(list -> {
                            pause(300);
                            return list;
                        });
                CompletableFuture<List<People>> people = DATA_SERVICE.readPeopleAsync(pool)
                        .thenApply(list -> {
                            pause(300);
                            return list;
                        });
                CompletableFuture<Void> printCars = cars.thenAccept(
                        list -> System.out.println("cars " + list.size()));
                CompletableFuture<Void> printPeople = people.thenAccept(
                        list -> System.out.println("people " + list.size()));
                System.out.println("other work on main");
                CompletableFuture.allOf(printCars, printPeople).join();
            } finally {
                shutdown(pool);
            }
        }
    }

    /**
     * FileReadService.readCarsAndPeopleAsync starts both reads and
     * thenCombine-s them. thenAccept prints one summary.
     * There is no get() between the two reads.
     */
//    static final class CombineCarsAndPeople {
//
//        static void demo() {
//            header("8. CombineCarsAndPeople — thenCombine, then thenAccept");
//            
//            ExecutorService pool = Executors.newFixedThreadPool(2);
//            try {
//                long start = System.currentTimeMillis();
//                CompletableFuture<LoadedFiles> both = files.readCarsAndPeopleAsync(pool)
//                        .thenApply(loaded -> {
//                            pause(300);
//                            return loaded;
//                        });
//                CompletableFuture<Void> printed = both.thenAccept(loaded -> {
//                    System.out.println(loaded.summary());
//                    System.out.println("merge elapsed ms " + (System.currentTimeMillis() - start));
//                });
//                System.out.println("other work on main");
//                printed.join();
//            } finally {
//                shutdown(pool);
//            }
//        }
//    }

    /**
     * A failed supplyAsync completes the future as exceptionally completed.
     * exceptionally() maps that error to a fallback value.
     * handle() can see both the value and the error in one callback.
     */
    static final class HandleReadError {

        static void demo() {
            header("9. HandleReadError — exceptionally() and handle()");
            ExecutorService pool = Executors.newFixedThreadPool(2);
            try {
                CompletableFuture<String> fallback = CompletableFuture
                        .<String>supplyAsync(() -> {
                            throw new IllegalStateException("cars.json missing");
                        }, pool)
                        .exceptionally(error -> "fallback: " + rootMessage(error));

                CompletableFuture<String> handled = CompletableFuture
                        .<String>supplyAsync(() -> {
                            throw new IllegalStateException("people.json missing");
                        }, pool)
                        .handle((value, error) -> {
                            if (error != null) {
                                return "handled: " + rootMessage(error);
                            }
                            return value;
                        });

                System.out.println(fallback.join());
                System.out.println(handled.join());
            } finally {
                shutdown(pool);
            }
        }
    }

    static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    static void shutdown(ExecutorService pool) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pool.shutdownNow();
        }
    }

    static List<Car> slowCars(DataService files) throws IOException {
        pause(300);
        return files.readCars();
    }

    static List<People> slowPeople(DataService files) throws IOException {
        pause(300);
        return files.readPeople();
    }

    static void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    static String rootMessage(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getMessage();
    }
}

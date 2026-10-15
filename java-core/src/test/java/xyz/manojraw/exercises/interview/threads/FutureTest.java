package xyz.manojraw.exercises.interview.threads;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Each class below shows a Future problem, then the next class fixes it.
 * Main calls every demo.
 *
 * 1. SequentialWorkOnMain runs slow calls on main, one after another.
 * 2. SubmitThenGetStillBlocks starts a pool thread, then get() makes main wait anyway.
 * 3. GetInSubmitOrder waits in start order, so a finished task sits behind a slow one.
 * 4. InvertedIsDoneLoop copies the people-poll bug: get() sits inside while (!isDone()).
 * 5. PollIsDoneWithSleep peeks with isDone(), sleeps between sweeps, prints as tasks finish.
 * 6. FailedTaskLooksFineUntilGet looks done and healthy until get() unwraps the real error.
 * 7. ShutdownThenSubmitFails shuts the pool down too early, then the next submit is rejected.
 *
 * PollIsDoneWithSleep is the Future shape to keep. The next file,
 * CompletableFutureTest, removes the poll loop.
 *
 * The write-up is notes/03_future.md.
 */
public class FutureTest {

    public static void main(String[] args) throws Exception {
        SequentialWorkOnMain.demo();
        SubmitThenGetStillBlocks.demo();
        GetInSubmitOrder.demo();
        InvertedIsDoneLoop.demo();
        PollIsDoneWithSleep.demo();
        FailedTaskLooksFineUntilGet.demo();
        ShutdownThenSubmitFails.demo();
        System.out.println();
        System.out.println("each Future demo behaved as notes/03_future.md describes");
    }

    /**
     * Slow calls run on main, one after another.
     * "Other work" prints only after every call returns.
     */
    static final class SequentialWorkOnMain {

        static void demo() {
            header("1. SequentialWorkOnMain — slow calls sit on main");
            SlowApi api = new SlowApi();
            long start = System.currentTimeMillis();
            for (int id = 1; id <= 3; id++) {
                System.out.println(api.getData(id));
            }
            System.out.println("other work on main");
            System.out.println("elapsed ms " + (System.currentTimeMillis() - start));
        }
    }

    /**
     * submit() starts the call on a pool thread.
     * get() still parks main until that one future is done.
     * "Other work" still prints last.
     */
    static final class SubmitThenGetStillBlocks {

        static void demo() {
            header("2. SubmitThenGetStillBlocks — get() parks main");
            ExecutorService pool = Executors.newFixedThreadPool(2);
            SlowApi api = new SlowApi();
            long start = System.currentTimeMillis();
            try {
                Future<String> future = pool.submit(() -> api.getData(3));
                System.out.println("submitted, main still here");
                System.out.println(future.get());
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
     * Several tasks run at once. get() still walks the list in submit order.
     * id 4 is slow and first in the list, so data-1 can already be done
     * while main is still inside get() for data-4.
     */
    static final class GetInSubmitOrder {

        static void demo() {
            header("3. GetInSubmitOrder — print order follows submit order");
            ExecutorService pool = Executors.newFixedThreadPool(3);
            SlowApi api = new SlowApi();
            List<Future<String>> futures = new ArrayList<>();
            long start = System.currentTimeMillis();
            try {
                for (int id = 4; id >= 1; id--) {
                    final int userId = id;
                    futures.add(pool.submit(() -> api.getData(userId)));
                }
                for (Future<String> future : futures) {
                    System.out.println(future.get());
                }
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
     * The first draft of the people poll looked like this:
     *
     *   while (!people.isDone()) {
     *       System.out.println(people.get());
     *   }
     *
     * Two outcomes:
     * already done -> the loop body never runs, so nothing prints.
     * still running -> get() waits, so isDone() did not help.
     */
    static final class InvertedIsDoneLoop {

        static void demo() throws Exception {
            header("4. InvertedIsDoneLoop — get() inside while (!isDone())");
            ExecutorService pool = Executors.newFixedThreadPool(2);
            SlowApi api = new SlowApi();
            try {
                Future<String> alreadyDone = pool.submit(() -> api.getData(1));
                alreadyDone.get();
                System.out.println("task is already done, isDone=" + alreadyDone.isDone());
                int prints = 0;
                while (!alreadyDone.isDone()) {
                    System.out.println("would print " + alreadyDone.get());
                    prints++;
                }
                System.out.println("prints while already done: " + prints);

                Future<String> stillRunning = pool.submit(() -> api.getData(3));
                long start = System.currentTimeMillis();
                while (!stillRunning.isDone()) {
                    System.out.println("get() inside the loop returned " + stillRunning.get());
                }
                System.out.println("that get() waited ms " + (System.currentTimeMillis() - start));
            } finally {
                shutdown(pool);
            }
        }
    }

    /**
     * Peek with isDone(). Remove a future only when it is done.
     * get() then returns at once. Sleep between sweeps so the loop
     * does not spin. "Other work" prints first. Results print in finish order.
     */
    static final class PollIsDoneWithSleep {

        static void demo() {
            header("5. PollIsDoneWithSleep — peek, sleep, print as they finish");
            ExecutorService pool = Executors.newFixedThreadPool(3);
            SlowApi api = new SlowApi();
            List<Future<String>> pending = new ArrayList<>();
            try {
                for (int id = 4; id >= 1; id--) {
                    final int userId = id;
                    pending.add(pool.submit(() -> api.getData(userId)));
                }
                System.out.println("other work on main");
                while (!pending.isEmpty()) {
                    Iterator<Future<String>> it = pending.iterator();
                    while (it.hasNext()) {
                        Future<String> future = it.next();
                        if (future.isDone()) {
                            it.remove();
                            try {
                                System.out.println("finished " + future.get());
                            } catch (ExecutionException e) {
                                System.out.println("task failed: " + e.getCause());
                            }
                        }
                    }
                    if (!pending.isEmpty()) {
                        Thread.sleep(50);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                pool.shutdownNow();
                return;
            } finally {
                shutdown(pool);
            }
        }
    }

    /**
     * A failed task still becomes isDone() true and isCancelled() false.
     * The real exception sits inside ExecutionException, and only get() throws it.
     */
    static final class FailedTaskLooksFineUntilGet {

        static void demo() throws Exception {
            header("6. FailedTaskLooksFineUntilGet — the error waits inside get()");
            ExecutorService pool = Executors.newFixedThreadPool(1);
            try {
                Future<String> future = pool.submit(() -> {
                    throw new IllegalStateException("disk error");
                });
                while (!future.isDone()) {
                    Thread.sleep(20);
                }
                System.out.println("isDone=" + future.isDone() + " isCancelled=" + future.isCancelled());
                try {
                    System.out.println(future.get());
                } catch (ExecutionException e) {
                    System.out.println("get() wraps the real error: " + e.getCause());
                }
            } finally {
                shutdown(pool);
            }
        }
    }

    /**
     * The first draft of this file used one shared pool and called shutdown()
     * at the end of the first method. The second method then called submit()
     * on a closed pool.
     */
    static final class ShutdownThenSubmitFails {

        static void demo() {
            header("7. ShutdownThenSubmitFails — one shared pool, shutdown too early");
            ExecutorService pool = Executors.newFixedThreadPool(2);
            pool.shutdown();
            try {
                pool.submit(() -> "next demo");
                System.out.println("submit after shutdown unexpectedly worked");
            } catch (RejectedExecutionException e) {
                System.out.println("submit after shutdown: " + e.getClass().getSimpleName());
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
            if (!pool.awaitTermination(3, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pool.shutdownNow();
        }
    }

    /**
     * Stands in for a slow HTTP or file call.
     * Sleep is id * 200 ms, then it returns "data-{id}".
     */
    static final class SlowApi {
        String getData(int userId) {
            try {
                Thread.sleep(userId * 200L);
                return "data-" + userId;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
    }
}

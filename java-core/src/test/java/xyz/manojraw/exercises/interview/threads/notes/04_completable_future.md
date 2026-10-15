# CompletableFuture: callbacks instead of a poll loop

`Future` gives you a ticket and `get()`. `CompletableFuture` is still a ticket. You can also attach work that runs when the result is ready: `thenAccept`, `thenApply`, `thenCombine`, `exceptionally`.

The first draft of this file was one `main` with most of the code commented out. That draft is now nine nested classes in `CompletableFutureTest`. Each class is one of those comments, turned into a run. The next class fixes it.

`FileReadService` is the service to keep. It wraps `FileService.readAndGetCars()` and `readAndGetPeople()`. The async methods take an `Executor` from the caller.

These checks match the `main` in `CompletableFutureTest`. The JSON files have 1000 cars and 1000 people. Demos that need a visible wait add a 300 ms pause around the read.

Read `03_future.md` first.

## Which one should you keep?

1. Put the reads in `FileReadService`. `readCarsAsync(executor)` and `readPeopleAsync(executor)` call `supplyAsync(..., executor)`.
2. Merge with `readCarsAndPeopleAsync`. That method uses `thenCombine`. Print with `thenAccept`. Call `join()` once at the end.
3. Pass the pool in. Create it in `main`. Shut it down in `finally`.
4. On a failure, chain `exceptionally()` or `handle()`.

| What is still wrong | The class that fixes it |
| --- | --- |
| Cars, then people, on main | `CallableThenGet` |
| `get()` after `submit` parks main | `InvertedPeoplePoll` tries `isDone()`, still calls `get()` wrong |
| `get()` inside `while (!isDone())` still waits | `CompletableFutureGetStillBlocks` |
| `get()` on a `CompletableFuture` still parks main | `ForkJoinPoolDaemonNeedsWait` |
| Common-pool workers are daemons | `SleepToHoldMain` holds main with a guess |
| `Thread.sleep` to keep the JVM alive | `OwnExecutorThenAccept` |
| Two callbacks, two prints, no merge | `CombineCarsAndPeople` |
| A failed read has no fallback | `HandleReadError` |

## 1. SequentialFileReads

**Issue:** The first lines were:

```java
var cars = FileService.readAndGetCars();
cars.forEach(System.out::println);

var people = FileService.readAndGetPeople();
people.forEach(System.out::println);
```

Main reads cars. Then main reads people. The two files do not depend on each other. They still wait in a line. "Other work on main" prints last. On this run elapsed time was 686 ms: two 300 ms pauses plus the JSON reads.

**Fix:** Run the two reads on a pool. The next class does that with `Callable`.

## 2. CallableThenGet

**Issue:** The first draft submitted two callables:

```java
Callable<List<Car>> futureCars = new Callable<List<Car>>() {
    @Override
    public List<Car> call() {
        try {
            return FileService.readAndGetCars();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
};
Future<List<Car>> cars = service.submit(futureCars);
```

Both reads now overlap. `cars.get()` still parks main. `people.get()` parks main again. "Other work on main" still prints last. A throw inside `call()` becomes `ExecutionException` on `get()`. You do not see it until that `get()`.

On this run elapsed time was 312 ms, the slower of the two pauses. The wait moved to `get()`.

**Fix:** The next class tries `isDone()` so main can do other work. It copies the people loop from the draft, which is the next bug.

## 3. InvertedPeoplePoll

**Issue:** The draft waited for cars with a busy loop, then polled people like this:

```java
while (true) {
    if (cars.isDone()) {
        System.out.println("Total car data " + cars.get().size());
        break;
    }
}

while (!people.isDone()) {
    System.out.println("Total people data " + people.get().size());
}
```

The draft submitted people only after the cars loop finished. The two reads did not overlap.

`people.get()` inside that `while` waits for the read. `isDone()` did not save you a wait. The demo prints one size line, and `that get() waited ms` is about 300.

If the people future is already done when the `while` starts, `isDone()` is true. The body never runs. The size never prints. `FutureTest.InvertedIsDoneLoop` shows that skip.

The cars loop also has no sleep. It burns a core until cars finish.

**Fix:** Stop polling. Use `CompletableFuture` and a callback. The next class still calls `get()`, which is the leftover wait.

## 4. CompletableFutureGetStillBlocks

**Issue:** `FileService` already had:

```java
public static CompletableFuture<List<Car>> cars = CompletableFuture.supplyAsync(() -> {
    try {
        return readAndGetCars();
    } catch (IOException e) {
        throw new RuntimeException(e);
    }
});
```

The draft then did `carsData.get()`. `get()` on a `CompletableFuture` waits the same way `Future.get()` waits. Main is parked. "Other work on main" prints last.

`supplyAsync` is the start of the fix. `get()` gives the callback style away.

**Fix:** `thenAccept` instead of `get()`. The next class does that on the common pool, and shows why main still has to wait at the end.

## 5. ForkJoinPoolDaemonNeedsWait

**Issue:** `supplyAsync(supplier)` with no executor uses `ForkJoinPool.commonPool()`. The JVM creates that pool on first use. Size is about `cores - 1`. Parallel streams use it too. A blocking file read on those workers slows other parallel streams in the same process.

Those workers are daemon threads. Daemon threads do not keep the JVM alive. When main returns, the JVM exits. A `thenAccept` that has not run yet is dropped.

The demo starts a 400 ms read, attaches `thenAccept`, and prints `printed=false` on the next line. `accept.join()` waits for the callback. Then `printed=true`. `join()` on the `thenAccept` future is what kept the process up. `cars.join()` only waits for the list. It does not wait for the callback. Remove the join, and the accept body is lost.

`join()` wraps a failure in `CompletionException`. `get()` wraps it in `ExecutionException`. For "wait at the end", `join()` is the usual call.

**Fix:** Pass your own pool, or `join()` before main returns. The next class tries a sleep instead of `join()`.

## 6. SleepToHoldMain

**Issue:** The draft did this:

```java
carsData.thenAccept(System.out::println);
System.out.println("Other task are running without blocking main thread");
Thread.sleep(6000);
```

The 6000 is a guess. A fast read wastes five seconds. A slow read can still miss the print. The demo sleeps 600 ms for a 200 ms read, so `printed=true` this time. That success depends on the numbers.

**Fix:** `join()` on the future you care about. Or use a pool of normal threads and shut it down in `finally`. That is the next class.

## 7. OwnExecutorThenAccept

**Issue:** Classes 5 and 6 either drop the callback, or guess a sleep.

**Fix:** `FileReadService.readCarsAsync(pool)` and `readPeopleAsync(pool)`.

```java
ExecutorService pool = Executors.newFixedThreadPool(2);
try {
    CompletableFuture<List<Car>> cars = files.readCarsAsync(pool);
    CompletableFuture<List<People>> people = files.readPeopleAsync(pool);
    CompletableFuture<Void> printCars = cars.thenAccept(
            list -> System.out.println("cars " + list.size()));
    CompletableFuture<Void> printPeople = people.thenAccept(
            list -> System.out.println("people " + list.size()));
    System.out.println("other work on main");
    CompletableFuture.allOf(printCars, printPeople).join();
} finally {
    pool.shutdown();
}
```

"Other work on main" prints first. The size lines print when each read finishes. `allOf(...).join()` waits for both callbacks. Pool threads from `newFixedThreadPool` are normal threads, so the JVM stays up until `shutdown()`.

Pass the executor in. `FileReadService` does not create a pool and does not shut one down.

This is the callback shape to keep.

**Issue still open:** Two callbacks, two prints. One summary of both lists needs a merge. That is the next class.

## 8. CombineCarsAndPeople

**Issue:** Class 7 prints cars and people on their own.

The draft also said: if you split a large file and merge at the end, each piece is another `Future` and another `get()`.

**Fix:** `thenCombine`. `FileReadService.readCarsAndPeopleAsync` is that merge:

```java
return readCarsAsync(executor).thenCombine(
        readPeopleAsync(executor),
        LoadedFiles::new
);
```

Both reads start. When both lists are ready, the combiner runs. `thenAccept` prints `cars=1000 people=1000`. There is no `get()` between the two reads.

`thenCombine` waits for both sides. `thenCompose` chains a second future that needs the first value. Use `thenCompose` when the second call needs an id from the first.

## 9. HandleReadError

**Issue:** `supplyAsync` that throws completes the future as failed. `join()` then throws `CompletionException`. The draft's `Callable` had the same hole: the error waited inside `get()`.

**Fix:**

```java
.exceptionally(error -> "fallback: " + error.getCause().getMessage())
```

`exceptionally` runs only on failure. It turns the error into a value. The future after it is successful with that fallback.

```java
.handle((value, error) -> {
    if (error != null) {
        return "handled: " + rootMessage(error);
    }
    return value;
})
```

`handle` always runs. You see the value and the error in one place.

The demo prints `fallback: cars.json missing` and `handled: people.json missing`. `CompletableFuture` wraps the throw, so the demo walks `getCause()` to the `IllegalStateException`.

## FileReadService

Keep the blocking reads, and the async wrappers, on this class:

- `readCars()` / `readPeople()` — caller thread, can throw `IOException`
- `readCarsAsync(executor)` / `readPeopleAsync(executor)` — `supplyAsync` on your pool
- `readCarsAndPeopleAsync(executor)` — `thenCombine` into `LoadedFiles`

`IOException` from `FileService` becomes `UncheckedIOException` inside `supplyAsync`. A `Supplier` cannot throw a checked exception.

`FileService.cars` and `FileService.people` start a common-pool read when the class loads. Prefer `FileReadService` and an executor you pass in.

## The common pool, in short

The JVM creates `ForkJoinPool.commonPool()` the first time something needs it. `supplyAsync` without an executor, and `list.parallelStream()`, both use it. Workers are daemons. A blocking read on that pool can stall parallel streams elsewhere.

A pool from `Executors.newFixedThreadPool(n)` uses normal threads. The JVM waits for them. You shut that pool down.

## Checklist

- Two independent reads go to `supplyAsync` on the same pool.
- Print with `thenAccept`. Merge with `thenCombine`.
- `join()` once at the end, or `allOf(...).join()`.
- Pass the executor in. Shut it down in `finally`.
- Chain `exceptionally` or `handle` for a failed read.
- Keep `FileReadService` free of `get()` in the middle of a chain.

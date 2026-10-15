# Future: start work, then wait for a result

`Future` is a ticket for work that is still running. `submit` hands a `Callable` to a pool and gives you that ticket. The result is ready later. You get it with `get()`.

The code is `FutureTest`. Each nested class fixes the class before it, and leaves a new gap open. `PollIsDoneWithSleep` is the Future shape to keep. `CompletableFutureTest` is the next step.

These checks match the `main` in `FutureTest`. `SlowApi.getData(id)` sleeps `id * 200` milliseconds, then returns `data-{id}`.

## Which one should you keep?

1. For several independent calls, submit them all first. Walk the list with `isDone()`. Print a result when that future is done. Sleep a little between sweeps. That class is `PollIsDoneWithSleep`.
2. Give each demo its own pool, or shut the pool down once at the end. `shutdown()` at the end of the first method is how `ShutdownThenSubmitFails` breaks the next submit.
3. When a task throws, call `get()` inside `try`. The real error is `e.getCause()` on `ExecutionException`.
4. When you want a callback instead of a poll loop, use `CompletableFuture`. That is the next note.

| What is still wrong | The class that fixes it |
| --- | --- |
| Slow calls run on main, one after another | `SubmitThenGetStillBlocks` |
| `get()` parks main for that one future | `GetInSubmitOrder` starts several, still waits in list order |
| Print order follows submit order | `PollIsDoneWithSleep` |
| `get()` sits inside `while (!isDone())` | `PollIsDoneWithSleep` |
| A failed task looks fine until `get()` | `FailedTaskLooksFineUntilGet` shows the wrap |
| `shutdown()` then `submit()` on the same pool | `ShutdownThenSubmitFails` |

## 1. SequentialWorkOnMain

**Issue:** `getData(1)`, then `getData(2)`, then `getData(3)` run on main. Each call sleeps. The next call waits. "Other work on main" prints after every sleep.

On this run the elapsed time was 1216 ms: 200 + 400 + 600, plus a little overhead.

**Fix:** Hand each call to a pool with `submit`. The next class does that, and then shows the new wait.

## 2. SubmitThenGetStillBlocks

**Issue:** `pool.submit(() -> api.getData(3))` starts the sleep on a pool thread. Main then calls `future.get()`. `get()` waits until that future is done. "Other work on main" still prints last. The elapsed time is still about 600 ms for id 3.

`submit` returns at once. The wait moved to `get()`.

**Fix:** Submit more than one task before the first `get()`. The next class does that.

## 3. GetInSubmitOrder

**Issue:** Four tasks go to a pool of three. The list order is id 4, 3, 2, 1. The `for` loop calls `get()` in that same order. `data-4` sleeps 800 ms. `data-1` is already done while main is still inside `get()` for `data-4`. The prints are `data-4`, `data-3`, `data-2`, `data-1`. "Other work on main" still prints last.

The pool did overlap the sleeps. The print loop hid that. On this run elapsed time was 806 ms, the sleep for id 4.

**Fix:** Peek with `isDone()`. Print a future when it is done. The next class first shows a broken peek.

## 4. InvertedIsDoneLoop

This is the people poll from the first `CompletableFutureTest` draft:

```java
while (!people.isDone()) {
    System.out.println("Total people data " + people.get().size());
}
```

**Issue:** Two outcomes, both bad.

Already done: `isDone()` is true. The `while` body never runs. Nothing prints. The demo does `alreadyDone.get()` first, then that loop. `prints while already done` is `0`.

Still running: the body calls `get()`. `get()` waits. `isDone()` did not save you a wait. The demo's second future takes about 600 ms inside that `get()`.

A busy `while (true)` that calls `isDone()` with no sleep has a third cost. The loop burns a core until the task finishes.

**Fix:** `if (future.isDone())` then `get()`, and sleep between sweeps. That is the next class.

## 5. PollIsDoneWithSleep

**Issue:** Class 4 either skipped the print, or blocked inside `get()`, or spun.

**Fix:**

```java
System.out.println("other work on main");   // first line, nothing waited

while (!pending.isEmpty()) {
    Iterator<Future<String>> it = pending.iterator();
    while (it.hasNext()) {
        Future<String> future = it.next();
        if (future.isDone()) {              // peek, does not wait
            it.remove();
            System.out.println("finished " + future.get()); // already done, returns at once
        }
    }
    if (!pending.isEmpty()) {
        Thread.sleep(50);                   // give the core a rest
    }
}
```

"Other work on main" prints first. The finish lines follow finish order. On this run that was `data-2`, `data-3`, `data-1`, `data-4`. Three pool threads start ids 4, 3, and 2 first, so id 1 waits for a free thread. `get()` after `isDone()` returns at once. `ExecutionException` is caught per future, so one failure leaves the others running.

`it.remove()` is the safe way to drop the current future during that iteration.

This is the Future shape to keep.

**Issue still open:** You still write the loop. You still sleep. A callback would run when the result is ready. That is `CompletableFuture`.

## 6. FailedTaskLooksFineUntilGet

**Issue:** The callable throws `IllegalStateException("disk error")`. After it finishes, `isDone()` is true and `isCancelled()` is false. The future looks healthy. The real error is stored inside the future.

**Fix:** `get()` throws `ExecutionException`. The callable's exception is `e.getCause()`.

```java
try {
    future.get();
} catch (ExecutionException e) {
    System.out.println("get() wraps the real error: " + e.getCause());
}
```

`InterruptedException` is a different catch. Restore the flag with `Thread.currentThread().interrupt()`.

`isDone()` true means the task finished, success or failure. It does not mean `get()` will return a value.

## 7. ShutdownThenSubmitFails

**Issue:** The first draft of `FutureTest` had one `static` pool. `futureThreadBlocking()` called `service.shutdown()` at the end. `futureThreadBlockingHandlingUsingIsDone()` then called `service.submit(...)`. A closed pool rejects new work.

**Fix:** Each demo creates its own pool and shuts that pool down in `finally`. If several methods share one pool, call `shutdown()` once, after the last submit.

`shutdown()` stops new submits and lets running tasks finish. `shutdownNow()` interrupts running tasks. `awaitTermination` waits for the close. Each demo uses that helper so pool threads do not keep the JVM alive after `main` returns.

Pool threads from `Executors.newFixedThreadPool` are normal threads. The JVM waits for them. That is why `shutdown()` belongs in `finally`.

## What Future still does not do

- There is no `thenAccept`. You poll, or you call `get()`.
- There is no `thenCombine`. Two futures stay two tickets. You join them yourself.
- A failure stays hidden until `get()`.
- `get()` with no timeout can wait forever.

`CompletableFuture` adds the callback, the combine, and `exceptionally()`. That write-up is `04_completable_future.md`.

## Checklist

- Submit all tasks before the first wait.
- Peek with `isDone()`. Call `get()` after that peek.
- Sleep between poll sweeps.
- Catch `ExecutionException` and print `getCause()`.
- Restore the interrupt flag on `InterruptedException`.
- Shut the pool down once, in `finally`.
- Keep one pool per demo, or share one pool and close it at the end.

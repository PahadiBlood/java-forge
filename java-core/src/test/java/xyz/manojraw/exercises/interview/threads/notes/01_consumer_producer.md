# Java Producer-Consumer with wait() / notify()

*Concepts, bugs we hit, and how each was fixed*

Exercise file: `xyz.manojraw.exercises.interview.threads.ConsumerProducerTest`. The final code below was compiled and run three times (Java 21) and completed all four orders each time. The two buggy structures in section 8 were also run and their real output is shown.


## 1. The goal

Simulate a coffee shop. A **producer** (`placeOrder`) puts orders on a queue. A **consumer** (`processOrder`) takes them off and brews each one on its own thread. Only `synchronized`, `wait()`, `notify()` and real threads are used.

- Producer and consumer never call each other. The shared queue is the only link.
- The consumer runs on its own long-lived background thread, started once, looping forever.
- Each order is brewed on its own thread (3 second sleep) so orders do not block each other.


## 2. Final clean code

Changes from your last version: the queue is renamed `orders` (it no longer clashes with the local list in `main`), the task is built outside the lock, the confirmation print is outside the lock, interrupts are restored, and the consumer thread has a name. The logic is the same as yours.

```java
package xyz.manojraw.exercises.interview.threads;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

/**
 * Producer-consumer coffee shop using synchronized + wait()/notify().
 *
 * Producer : placeOrder()   -> adds a task to the queue, then notifies.
 * Consumer : processOrder() -> waits for tasks, drains the queue, starts one thread per task.
 * The two sides never call each other. The shared queue is their only link.
 */
public class ConsumerProducerTest {

    // Shared queue AND the lock object. Both sides must synchronize on this same instance.
    private static final Queue<Runnable> orders = new ArrayDeque<>();

    /** Producer side. Runs on whichever thread calls it (main, in this demo). */
    public String placeOrder(String name) {
        // Built outside the lock: the lock should protect only the queue, nothing else.
        Runnable brewTask = () -> {
            try {
                // Naming inside the task avoids the "start() returns before setName()" race.
                Thread.currentThread().setName(name);
                Thread.sleep(3000); // simulate brewing
                System.out.println("Order is completed for customer " + name);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        synchronized (orders) {          // must be the SAME object the consumer waits on
            orders.add(brewTask);
            orders.notify();             // wake the consumer; lock is released only when this block ends
        }
        System.out.println("Coffee ordered successfully for customer " + name);
        return UUID.randomUUID().toString();
    }

    /** Consumer side. Meant to run forever on its own dedicated thread. */
    public void processOrder() throws InterruptedException {
        while (true) {                       // OUTSIDE synchronized: lock is released and re-acquired each pass
            synchronized (orders) {
                while (orders.isEmpty()) {   // while, not if: re-check after every wake-up
                    orders.wait();           // releases the lock while parked
                }
                while (!orders.isEmpty()) {  // drain everything that is queued
                    Thread.startVirtualThread(orders.poll()); // already started: do NOT call start() again
                }
            }
        }
    }

    public static void main(String[] args) {
        ConsumerProducerTest shop = new ConsumerProducerTest();

        // Consumer must be a NON-daemon platform thread: it keeps the JVM alive
        // while the (always-daemon) virtual threads finish brewing.
        Thread consumer = new Thread(() -> {
            try {
                shop.processOrder();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "order-consumer");
        consumer.start();

        System.out.println("Order place started");
        List.of("Manoj", "Yoga", "Chamal", "GoGo").forEach(shop::placeOrder);
        // main ends here; the program keeps running because the consumer never returns.
    }
}
```

Expected output (order of the two groups is stable; the order of lines inside a group can vary):

```text
Order place started
Coffee ordered successfully for customer Manoj
Coffee ordered successfully for customer Yoga
Coffee ordered successfully for customer Chamal
Coffee ordered successfully for customer GoGo
Order is completed for customer Manoj
Order is completed for customer Yoga
Order is completed for customer Chamal
Order is completed for customer GoGo
```

> **Note:** The program never exits on its own. The consumer is a non-daemon thread stuck in `while (true)`. Stop it manually, or add a shutdown flag if you want a clean exit.


## 3. Design rules that came out of the discussion


### 3.1 Producer and consumer must be independent

**Problem.** Early drafts had `placeOrder` trigger processing directly, or branched on queue size. That couples the two sides and reintroduces race conditions.

**Fix.** `placeOrder` only adds and notifies. `processOrder` only waits and drains. Nothing else connects them.


### 3.2 The consumer is one dedicated background thread

**Problem.** Starting a consumer per order wastes threads and loses the point of the pattern.

**Fix.** Start one consumer thread in `main`, once. It loops forever. Calling `processOrder()` directly on `main` would block `main` forever inside `wait()`.


### 3.3 wait() and notify() need synchronized on the same object

**Problem.** `IllegalMonitorStateException`. A `synchronized` method locks `this`, but the code called `wait()` on the queue. A bare `wait()` also means `this.wait()`.

**Fix.** Use `synchronized (orders)` blocks in both methods and call `orders.wait()` / `orders.notify()`. Rule: you can only call `x.wait()` or `x.notify()` while holding `x`'s lock.


### 3.4 Why a synchronized block and not a synchronized method

- A synchronized method always locks `this` (or the Class for static). It cannot lock a specific shared object.
- wait/notify needed the queue as the lock, so a block was required.
- A block also lets you lock only the critical section, not the whole method.


### 3.5 wait() goes in a while loop, never an if

**Problem.** After waking, another thread may already have taken the item (competing consumers), or the wake-up may be spurious. An `if` would continue on a false assumption.

**Fix.** `while (orders.isEmpty()) orders.wait();` re-checks the condition every time the thread wakes.


## 4. How the lock really behaves

| Action | Releases the lock? | What actually happens |
|---|---|---|
| wait() | Yes | Thread parks and gives the lock up. On wake it must re-acquire the lock before continuing. |
| notify() | No | Only marks one waiter as eligible. Notifier keeps the lock until its synchronized block ends. |
| Thread.sleep() | No | Holder keeps the lock. Every other thread wanting it blocks. |
| End of synchronized block | Yes | Lock is free. Waiters and other threads compete for it. |
| thread.start() | n/a | Returns immediately. The new thread runs later, concurrently. |


### 4.1 Who is the holder?

- The lock belongs to the **object** (`orders`). The **thread** is the holder. At any moment: one holder or none.
- Here the holder alternates between `th` (consumer) and `main` (producer). It does not matter which method runs; only which thread currently owns the monitor.
- `orders` is `static`, so there is exactly one object and one lock shared by every thread. With a non-static field and two `ConsumerProducerTest` instances, each would have its own queue and its own lock, and nothing would be shared.


### 4.2 Who gets the lock next?

When `main` leaves its synchronized block, both `main` (placing the next order) and `th` (woken, wanting to resume) compete for it. The JVM scheduler decides. This is non-deterministic ordering, not a data race: `synchronized` guarantees only one thread is inside at a time, so nothing is corrupted or lost. Only the order of console lines can change. (Your metro-seat analogy fits: several people eye one seat, one gets it, nobody is wronged.)


### 4.3 The sleep-inside-the-lock experiment

You added a long `Thread.sleep` inside the consumer's synchronized block. Because sleep does not release the lock, `placeOrder` on `main` could not even enter its own synchronized block until the sleep ended. That is why orders appeared delayed, and why this sleep could never create the "gap" you were trying to engineer: while the consumer holds the lock, nobody can add to the queue.

> **Note:** Rule of thumb: keep as little as possible inside a synchronized block. Anything slow or stuck in there freezes every thread that needs the same lock.


## 5. The thread-name and join problem

**Problem.** The completion print used `Thread.currentThread().getName()` from the consumer thread, so it printed "Thread-0" instead of the customer name. Also `t.start()` returns immediately, so `setName` in the new thread may not have run yet.

**Options discussed.** `t.join()` waits for the worker to finish (correct but blocks the consumer). `Thread.sleep` to "wait long enough" is fragile and only acceptable in throwaway tests.

**Fix used.** Do the naming and the "order completed" print inside the task itself, after its own sleep. No `join()` needed, and the consumer never blocks on brewing.


## 6. Lambdas, captured variables and effectively final

- A lambda is just code packaged as an object. It runs on whatever thread calls it. `r.run()` runs on the current thread; only `new Thread(r).start()` or `Thread.startVirtualThread(r)` moves it to another thread.
- `list.forEach(lambda)` runs synchronously on the calling thread. Parallelism only appears with `.parallelStream()` / `.parallel()`.
- Local variables captured by a lambda are **copied at lambda creation time**. Reassigning the variable later would leave the lambda with a stale copy, so Java requires captured locals to be final or effectively final.
- For objects the copied value is the **reference**. The variable cannot be re-pointed, but the object's contents can still change. That is allowed by the compiler and is a real shared-mutable-state hazard across threads.
- Fields (static or instance) are not copied. The lambda reads their current value through the object or class, so they have no such restriction. That is why `orders` works inside the Runnable.
- The check is compile-time and uniform. Even `x++` after a synchronous `forEach` that used `x` is rejected, because the compiler does not analyse how each receiving method uses the lambda (it might store it and run it later on another thread).


## 7. Daemon threads and virtual threads

- The JVM exits when no **non-daemon** thread is alive. Daemon threads do not keep it alive and are cut off abruptly at exit.
- A virtual thread is a lightweight thread managed by the JVM. Daemon-ness is a separate flag.
- Virtual threads are **always daemon**. `setDaemon(false)` cannot change that (JEP 444; calling it throws `IllegalArgumentException`).
- `main` finishes almost instantly. If the consumer were also a daemon or virtual thread, the JVM would exit right away and orders would be cut off.
- So the consumer `th` stays a normal platform thread (non-daemon). It keeps the JVM alive while the virtual brewing threads finish.
- Virtual threads suit the brew tasks well: they mostly sleep, and blocked virtual threads are cheap.


### 7.1 The double-start bug

```java
Thread.startVirtualThread(orders.poll()).start();   // WRONG
```

`startVirtualThread` already starts the thread. The extra `start()` throws `IllegalThreadStateException`, which the lambda in `main` does not catch (it only catches `InterruptedException`). The consumer dies after the first order, the same symptom as the structural bug below.

```java
Thread.startVirtualThread(orders.poll());          // correct
```


## 8. The structural bug (the main lesson)


### 8.1 Original shape (no outer loop)

```java
synchronized (customers) {
    while (customers.isEmpty()) {
        customers.wait();
        while (customers.iterator().hasNext()) { ... poll and start ... }
    }
}   // method returns here
```

**What goes wrong.** After one wake-up and drain, the outer condition is re-checked. If it is true the thread waits again (fine). But if the queue is non-empty when the check happens, the loop exits and `processOrder()` **returns**. The consumer thread is dead. Any later order is added to the queue and nobody ever processes it.

Real output when orders exist before the consumer reaches the wait, then a later order arrives:

```text
ordered Manoj
ordered Yoga
[consumerB method RETURNED - consumer thread is dead]
--- later order arrives
ordered Chamal
consumer alive? false
```


### 8.2 Adding while(true) is not enough if the drain stays nested

```java
while (true) {
    synchronized (orders) {
        while (orders.isEmpty()) {
            orders.wait();
            while (!orders.isEmpty()) {          // drain is INSIDE the wait loop
                Thread.startVirtualThread(orders.poll());
            }
        }
    }
}
```

Now the thread never dies, but the drain only runs after a `wait()`. If the queue is already non-empty when the consumer arrives, `isEmpty()` is false, the body is skipped, and `while (true)` spins forever without processing anything (a busy loop holding and releasing the lock).

```text
ordered Manoj
ordered Yoga
--- later order arrives
ordered Chamal
consumer alive? true        <- alive, but not one "completed" line was ever printed
```


### 8.3 The fix

```java
while (true) {
    synchronized (orders) {
        while (orders.isEmpty()) {               // this loop ONLY waits
            orders.wait();
        }
        while (!orders.isEmpty()) {              // draining is a separate step
            Thread.startVirtualThread(orders.poll());
        }
    }
}
```

- The inner loop only waits. Draining is a separate step after it, so it runs whether or not a wait was needed.
- `while (true)` is **outside** `synchronized`. The lock is acquired and released fresh each pass, so the producer can always get in between passes. If `while (true)` were inside the block, the consumer would hold the lock forever and `placeOrder` could never add or notify.
- The outer loop exists to keep the consumer alive as a long-running background worker.


### 8.4 Why your staggered-sleep test passed

With orders placed one at a time after the consumer was already waiting, every pass went wait -> drain -> queue empty -> wait again. The buggy shape only fails when the queue is non-empty at the moment the consumer checks, for example orders placed before it starts, or a method that returns. The original test never produced that situation, so it looked fine.


### 8.5 Alternatives you tried and why they were rejected

- **Producer decides whether to trigger the consumer based on queue size.** Re-couples the two sides and adds races.
- **`processOrder()` calling itself recursively.** Unbounded recursion ends in `StackOverflowError`.
- **Calling `processOrder` from `placeOrder` to force the failure.** That removes the wait/notify design you are testing.


### 8.6 Correction to an earlier claim

> **Note:** Earlier in the session a "narrow race window" was suggested: a notify firing between the end of the drain and the next wait(). That window does not exist. The consumer holds the lock continuously through the drain and the re-check, so `placeOrder` cannot add anything in between. That is why you could not reproduce it. The real defect is the structure in 8.1 and 8.2, which is deterministic once the queue is non-empty at the check.


## 9. ReentrantLock and Condition (next step, sketch only)

`ReentrantLock` is an object, not a property of a class. Create **one** shared instance; both producer and consumer must use that same instance. It takes no constructor argument and does not "lock on" `orders`. Use `lock.newCondition()` instead of `wait()`/`notify()`.

```java
private static final ReentrantLock lock = new ReentrantLock();
private static final Condition notEmpty = lock.newCondition();

// producer
lock.lock();
try {
    orders.add(task);
    notEmpty.signal();          // replaces orders.notify()
} finally {
    lock.unlock();              // always in finally
}

// consumer
while (true) {
    lock.lock();
    try {
        while (orders.isEmpty()) notEmpty.await();   // replaces orders.wait()
        while (!orders.isEmpty()) Thread.startVirtualThread(orders.poll());
    } finally {
        lock.unlock();
    }
}
```

- `unlock()` must be in `finally`, because there is no automatic release at a closing brace like `synchronized` gives.
- Gains over `synchronized`: `tryLock()`, timeouts, interruptible locking, fairness, and several Conditions per lock.
- It does not change correctness here: the same rule applies, check the condition in a `while` loop under the lock.

> **Note:** This sketch was not compiled or run. Treat it as a starting point for your own implementation.


## 10. Known limits of this demo

- `notify()` wakes one waiter. With a single consumer that is fine. With several consumers prefer `notifyAll()` or a separate Condition.
- The queue is unbounded and one virtual thread is started per order. A real system would cap concurrency (a pool or semaphore).
- No shutdown mechanism: add a volatile flag or interrupt the consumer.
- Interrupts are restored with `Thread.currentThread().interrupt()` rather than swallowed.


## 11. Quick checklist

- Producer and consumer share only the queue.
- Same lock object on both sides; wait/notify called while holding it.
- Always `while (condition) wait();`, never `if`.
- Wait loop only waits; drain is a separate step.
- `while (true)` outside the synchronized block.
- Never `start()` a thread that `startVirtualThread` already started.
- Keep one non-daemon thread alive until work finishes.
- Keep synchronized blocks short; no sleeping or blocking inside.


## 12. The bounded queue to use after this one

`ArrayBlockingQueue` already contains the lock, the full/empty waits, and the bound this demo leaves to you. The comparison, and the file that uses it, is `02_array_blocking_queue.md`.
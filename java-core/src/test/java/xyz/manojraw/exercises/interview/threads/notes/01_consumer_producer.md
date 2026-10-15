# Producer and consumer with wait() and notify()

This note is the old way. You write the queue, the lock, `wait()`, and `notify()` yourself.

The code is `ConsumerProducerTest`. The final shape was compiled and run three times on Java 21. All four orders finished each time. The two broken loop shapes in the main lesson were also run. Their real output is below.

Read this note first. The next note, `02_array_blocking_queue.md`, is the better fix.

## The goal

Think of a coffee shop.

- The **producer** is `placeOrder`. It puts an order on a queue.
- The **consumer** is `processOrder`. It takes orders off the queue and brews them.

The two sides never call each other. The shared queue is the only link.

The consumer is one background thread. It starts once and loops forever. Each order is brewed on its own thread, with a 3 second sleep, so one order does not block the next.

The program does not stop by itself. The consumer thread never returns. Stop it by hand.

## The shape to keep

The full class is `ConsumerProducerTest`. These are the two parts that matter.

Producer. Build the work outside the lock. Inside the lock, only add and wake:

```java
synchronized (orders) {
    orders.add(brewTask);
    orders.notify();
}
```

Consumer. The forever loop is outside the lock. Inside the lock, wait first, then drain:

```java
while (true) {
    synchronized (orders) {
        while (orders.isEmpty()) {
            orders.wait();
        }
        while (!orders.isEmpty()) {
            Thread.startVirtualThread(orders.poll());
        }
    }
}
```

`orders` is an `ArrayDeque`. It is both the queue and the lock. Both sides must use this same object.

A normal run prints the four "ordered" lines, then the four "completed" lines. The lines inside each group can swap. The program then keeps running.

## 1. The two sides called each other

**Issue:** Early drafts made `placeOrder` start the work itself, or they checked the queue size and then decided what to do. The two sides were tied together. That brings races back.

**Fix:** `placeOrder` only adds and calls `notify`. `processOrder` only waits and drains. Nothing else connects them.

## 2. A new consumer thread for every order

**Issue:** Starting one consumer per order wastes threads. It also misses the point. One consumer should serve the queue.

**Fix:** Start one consumer thread in `main`, once. It loops forever.

Do not call `processOrder()` on `main`. `main` would sit inside `wait()` forever and never place the orders.

## 3. wait() and notify() used the wrong lock

**Issue:** This throws `IllegalMonitorStateException`. A `synchronized` method locks `this`. The code called `wait()` on the queue. A bare `wait()` means `this.wait()`. You may call `x.wait()` or `x.notify()` only while you hold the lock on `x`.

**Fix:** Use `synchronized (orders)` in both methods. Call `orders.wait()` and `orders.notify()`.

A `synchronized` method always locks `this` (or the class, if the method is static). It cannot lock the queue. A block can lock the queue, and it can cover only the few lines that touch the queue.

## 4. wait() was inside an if

**Issue:** After a thread wakes up, the item may already be gone. Another consumer may have taken it. Or the thread woke up even though nobody called `notify`. An `if` would continue as if the queue had an item.

**Fix:** Use a `while` loop. Check the queue again every time the thread wakes:

```java
while (orders.isEmpty()) {
    orders.wait();
}
```

## 5. What the lock actually does

The lock belongs to the object `orders`. A thread holds it. At one moment, one thread holds it, or nobody does.

Here the holder switches between `main` (the producer) and the consumer thread. The method name does not matter. What matters is which thread owns the lock.

`orders` is `static`, so there is one queue and one lock. If the field were not static, two shop objects would have two queues. They would not share anything.

| Action | Does it give the lock up? | What happens |
| --- | --- | --- |
| `wait()` | Yes | The thread sleeps and gives the lock up. When it wakes, it must get the lock again before it continues |
| `notify()` | No | It marks one waiting thread as ready. The caller keeps the lock until the block ends |
| `Thread.sleep()` | No | The holder keeps the lock. Everyone else who wants it waits |
| End of the `synchronized` block | Yes | The lock is free. Waiting threads compete for it |
| `thread.start()` | Not about the lock | It returns at once. The new thread runs later |

**Issue:** A long `Thread.sleep` inside the consumer's `synchronized` block looks like a pause. It is not a useful pause. Sleep does not give the lock up. `placeOrder` cannot enter its own block until the sleep ends. Nobody can add to the queue while the consumer holds the lock.

**Fix:** Keep the `synchronized` block short. Do slow work outside it. Brewing sleeps inside the task, after the task has left the lock.

When `main` leaves its block, `main` and the consumer both want the lock. The JVM picks one. Only one is inside at a time, so no order is lost. Only the order of the printed lines can change.

## 6. The completion line printed the wrong name

**Issue:** The "order completed" print used the consumer thread's name, so it printed `Thread-0` instead of the customer name. `t.start()` also returns at once, so `setName` in the new thread may not have run yet. `t.join()` would wait for the brew to finish, but then the consumer is blocked. `Thread.sleep` to "wait long enough" is fragile.

**Fix:** Set the name and print "order completed" inside the brew task, after its own sleep. The consumer does not wait for the brew.

## 7. A lambda and a captured variable

A lambda is code packed as an object. It runs on the thread that calls it. `startVirtualThread` is what moves it to another thread. `forEach` runs on the current thread.

**Issue:** A lambda copies a local variable when the lambda is created. If you change that variable later, the lambda would still have the old copy. Java rejects that. The variable must be final, or it must never be assigned again.

**Fix:** Do not reassign the local. For an object, the copy is the reference. You cannot point the variable at a new object, but you can still change the object's fields. That is allowed, and it is a real shared-data hazard.

Fields are not copied. The lambda reads the current field. That is why `orders` is fine inside the task.

The check is at compile time. Even `x++` after a normal `forEach` that used `x` is rejected. The compiler does not try to guess whether the lambda runs now or later.

## 8. The program exited before the coffee was ready

**Issue:** The JVM exits when no normal (non-daemon) thread is still alive. Daemon threads do not keep it alive. They are cut off.

A virtual thread is a light thread managed by the JVM. Virtual threads are always daemon. `setDaemon(false)` throws `IllegalArgumentException`.

`main` finishes almost at once. If the consumer were also a daemon or a virtual thread, the JVM would exit and the orders would be cut off.

**Fix:** Keep the consumer as a normal platform thread. It keeps the JVM alive while the virtual brew threads finish. Virtual threads are a good fit for the brew tasks, because those tasks mostly sleep.

## 9. start() was called twice

**Issue:**

```java
Thread.startVirtualThread(orders.poll()).start();   // wrong
```

`startVirtualThread` already starts the thread. The extra `start()` throws `IllegalThreadStateException`. The lambda in `main` does not catch that. The consumer dies after the first order. That looks like the loop bug in the next section.

**Fix:**

```java
Thread.startVirtualThread(orders.poll());
```

## 10. The consumer died, or it spun and did no work

This is the main lesson.

### Wrong shape: no outer loop

```java
synchronized (customers) {
    while (customers.isEmpty()) {
        customers.wait();
        while (customers.iterator().hasNext()) {
            // poll and start
        }
    }
}
// the method returns here
```

**Issue:** After one wake-up and one drain, the outer check runs again. If the queue is empty, the thread waits again. That part is fine. If the queue is not empty when the check happens, the loop ends and `processOrder()` returns. The consumer thread is dead. Later orders sit in the queue and nobody takes them.

Real output when orders exist before the consumer reaches `wait`, and a later order arrives:

```text
ordered Manoj
ordered Yoga
[consumer method RETURNED - consumer thread is dead]
--- later order arrives
ordered Chamal
consumer alive? false
```

### Still wrong: while(true), but the drain is inside the wait loop

```java
while (true) {
    synchronized (orders) {
        while (orders.isEmpty()) {
            orders.wait();
            while (!orders.isEmpty()) {          // drain is inside the wait loop
                Thread.startVirtualThread(orders.poll());
            }
        }
    }
}
```

**Issue:** The thread no longer dies. But the drain runs only after a `wait()`. If the queue already has items when the consumer arrives, `isEmpty()` is false, the body is skipped, and `while (true)` spins forever. It holds and releases the lock, and it never brews.

```text
ordered Manoj
ordered Yoga
--- later order arrives
ordered Chamal
consumer alive? true
```

No "completed" line is printed.

### The fix

```java
while (true) {
    synchronized (orders) {
        while (orders.isEmpty()) {               // this loop only waits
            orders.wait();
        }
        while (!orders.isEmpty()) {              // draining is a separate step
            Thread.startVirtualThread(orders.poll());
        }
    }
}
```

- The inner loop only waits. The drain is a separate step, so it runs even when no wait was needed.
- `while (true)` is **outside** `synchronized`. The lock is taken and given up on every pass, so the producer can get in. If `while (true)` were inside the block, the consumer would hold the lock forever. `placeOrder` could never add or notify.
- The outer loop keeps the consumer alive.

**Why a slow test looked fine:** If you place one order at a time, and the consumer is already waiting, every pass is wait, then drain, then wait again. The broken shapes fail when the queue is not empty at the moment of the check. For example, orders placed before the consumer starts, or a method that returns. The slow test never made that happen.

**Other tries that were rejected:**

- Let the producer decide whether to start the consumer based on queue size. That ties the two sides together again and adds races.
- Let `processOrder()` call itself. The stack grows until `StackOverflowError`.
- Call `processOrder` from `placeOrder` just to force the failure. That removes the wait/notify design you are testing.

The consumer holds the lock through the drain and the next check. The producer cannot add an item in that gap. There is no tiny race there. The real defect is the loop shape above.

## 11. Another fix: ReentrantLock

**Issue:** `synchronized` is tied to a block. It always unlocks at the closing brace. You cannot ask "try this lock", set a timeout, or have two separate wait lists on one lock.

**Fix:** `ReentrantLock` is an object you create. Both sides must use the same instance. It does not lock on `orders`. You use `lock.newCondition()` instead of `wait()` and `notify()`.

```java
private static final ReentrantLock lock = new ReentrantLock();
private static final Condition notEmpty = lock.newCondition();

// producer
lock.lock();
try {
    orders.add(task);
    notEmpty.signal();
} finally {
    lock.unlock();
}

// consumer
while (true) {
    lock.lock();
    try {
        while (orders.isEmpty()) {
            notEmpty.await();
        }
        while (!orders.isEmpty()) {
            Thread.startVirtualThread(orders.poll());
        }
    } finally {
        lock.unlock();
    }
}
```

`unlock()` must be in `finally`. There is no automatic unlock at a closing brace.

You also get `tryLock()`, timeouts, interruptible locking, a fair lock, and more than one `Condition` on the same lock. The rule does not change: check the condition in a `while` loop while you hold the lock.

This sketch was not compiled or run. Use it as a starting point.

The queue in the next note already uses this idea inside `ArrayBlockingQueue`. You usually call `put` and `take` instead of writing this yourself.

## What this demo still does not do

- `notify()` wakes one waiter. One consumer is fine. Several consumers should use `notifyAll()`, or a separate `Condition`.
- The queue has no size limit. A fast producer can grow it forever. One virtual thread starts per order. A real system would cap that.
- There is no clean stop. Add a flag, or interrupt the consumer.
- An interrupt is put back with `Thread.currentThread().interrupt()`. It is not ignored.

## Checklist

- The producer and the consumer share only the queue.
- Both sides lock the same object. `wait` and `notify` run while that lock is held.
- Always `while (condition) wait();`. Never `if`.
- The wait loop only waits. The drain is a separate step.
- `while (true)` is outside the `synchronized` block.
- Do not call `start()` on a thread that `startVirtualThread` already started.
- Keep one normal thread alive until the work is done.
- Keep `synchronized` blocks short. Do not sleep inside them.

## Next note

`ArrayBlockingQueue` already has the lock, the full and empty waits, and a size limit. That comparison is `02_array_blocking_queue.md`.

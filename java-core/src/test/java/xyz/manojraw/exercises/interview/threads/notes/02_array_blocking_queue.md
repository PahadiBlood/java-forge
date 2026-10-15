# Producer and consumer with ArrayBlockingQueue

This is the fix you keep after the hand-written queue is clear.

Read `01_consumer_producer.md` first. That file is `ConsumerProducerTest`. This file is `ConsumerProducerViaArrayBlockingQueueTest`.

These checks match OpenJDK 25. `ArrayBlockingQueue` has worked this way since it was added to `java.util.concurrent`.

## What this demo does

One producer thread calls `put` with `0`, then `1`, and so on up to `999`, then it repeats. One consumer thread calls `take`, prints the number, and sleeps for one second. The queue holds 5 numbers.

The consumer is the slow side, because of that sleep. On a run here the log started `Added num 0`, `Take num 0`, then `Added num 1` through `Added num 5` while the consumer slept. Those five numbers filled the queue. The next `put` waited until `Take num 1` freed a slot. After that the lines take turns, one take and one add.

An empty queue makes `take` wait in the same way. The consumer does not spin, and it never reads a number that was not `put`.

`main` gives both tasks to a pool of two threads, then `main` returns. Pool threads are normal threads, so the program stays up. There is no shutdown. Stop it by hand, the same way as the coffee-shop demo.

## The hand-written version, and what was wrong with it

**Issue:** `ConsumerProducerTest` shares an `ArrayDeque` and uses that same object as the lock:

```java
synchronized (orders) {
    orders.add(brewTask);
    orders.notify();
}
```

```java
synchronized (orders) {
    while (orders.isEmpty()) {
        orders.wait();
    }
    while (!orders.isEmpty()) {
        Thread.startVirtualThread(orders.poll());
    }
}
```

That is correct for one consumer and a queue that may grow forever. Every bug in the first note is a way to get those few lines wrong:

- waiting on a different object
- `if` around `wait`
- sleeping while the lock is held
- one `notify` when several threads are waiting
- the consumer method returning
- the drain sitting inside the wait loop

A queue with a size limit needs a second wait. The producer must wait when the queue is full. The consumer must wait when the queue is empty. `ArrayBlockingQueue` already does that. Inside it, the shape is:

```java
// producer
while (count == capacity) {
    notFull.await();          // wait while the array is full; the lock is given up
}
items[putIndex] = value;
notEmpty.signal();            // one taker may continue

// consumer
while (count == 0) {
    notEmpty.await();
}
value = items[takeIndex];
notFull.signal();             // one putter may continue
```

`signal` wakes one waiter on that condition. A full queue wakes a producer. An empty queue wakes a consumer. The `while` runs again after every wake, so a surprise wake, or a slot someone else already took, sends the thread back to `await`. The lock is given up for the whole wait. Your code that calls `put` and `take` does not see any of this.

**Fix:** Call `put` and `take` on `new ArrayBlockingQueue<>(5)`.

## Why this is the one to keep

### The queue has a size

**Issue:** The coffee-shop `ArrayDeque` has no limit. A fast producer grows the deque, and one virtual thread starts per order.

**Fix:** `new ArrayBlockingQueue<>(5)` makes an array of 5 slots. `put` waits while 5 items are in it. The sixth item waits. The heap stays one small array.

### The lock rules are inside the class

**Issue:** In the hand-written queue you can wait on the wrong object, use `if` where a `while` belongs, or forget to signal. The first note spent a long time on those bugs.

**Fix:** `put` and `take` take the same lock, wait on a condition, and signal the other side. Callers cannot make those mistakes.

### Your slow work does not hold the lock

**Issue:** `Thread.sleep` inside `synchronized (orders)` holds the lock. `wait` gives the lock up. `sleep` does not. The producer cannot add while that sleep runs.

**Fix:** `take` returns, and then `Thread.sleep(1000)` runs. The queue lock was already released inside `take`, so the producer can `put` during that second.

### Many producers and many consumers can share one queue

**Issue:** `orders.notify()` is right for one consumer. Add a second consumer, or a producer that also waits because the queue is full, and one `notify` can wake the wrong side.

**Fix:** Each `put` signals one taker. Each `take` signals one putter. The two conditions keep those waits apart.

### The value from put is visible to the thread that takes it

**Issue:** A hand-written queue has that promise only while every read and write of the deque stays inside `synchronized (orders)`. A read that forgets the block can see a stale or broken queue. `ArrayDeque` itself has no lock.

**Fix:** The unlock at the end of `put` happens before the lock at the start of the matching `take`. The taker sees the value that was put.

### null is rejected

**Issue:** An `ArrayDeque` accepts `null`. If your code uses `null` to mean "stop", the consumer cannot tell an empty queue from a null item.

**Fix:** `put(null)` fails at once. `ArrayBlockingQueue` does not allow null.

### Interrupt and timeout are already there

**Issue:** The hand-written loop must build a timeout from `wait(millis)`, or from `Condition.await(timeout, unit)`.

**Fix:** `put` and `take` wait in a way that an interrupt throws `InterruptedException` and clears the flag. The demo puts the flag back with `Thread.currentThread().interrupt()` and returns. That is the same restore the coffee-shop note uses. `offer` and `poll` add a timeout when a caller should give up instead of waiting.

### Fairness is a constructor flag

**Issue:** A `synchronized` block has no fair switch. A new thread can cut in front of a thread that is already waiting.

**Fix:** `new ArrayBlockingQueue<>(5)` allows that cutting in. `new ArrayBlockingQueue<>(5, true)` lines the waiters up. Fair mode does more lock work. This demo uses the default.

## What it does not fix

**Issue:** If the exercise is "show me `wait` and `notify`", this class hides them.

**Fix for that exercise:** Write `ConsumerProducerTest`. Use `ArrayBlockingQueue` when you want the buffer, and you want the code to focus on the work.

**Issue:** The coffee shop also starts a virtual thread per order. The queue only hands the task over. `ArrayBlockingQueue` does not start threads and does not brew.

**Fix:** A matching coffee shop would `put` the brew task into a queue of size 5. The consumer would `take`, then call `startVirtualThread`. The limit applies to the backlog. That is the cap the first note asked for.

**Issue:** The pool in this demo is never shut down. Both loops run until the thread is interrupted.

**Fix:** A real stop is `shutdownNow` on the executor, which interrupts the workers, or a special "stop" value the consumer understands. The queue will not invent that for you.

**Issue:** `ArrayBlockingQueue` uses one lock for both ends. If the buffer is large and both ends stay busy, that one lock is the limit.

**Fix:** `LinkedBlockingQueue` with a capacity you choose uses two locks, so a producer and a consumer overlap more. Its no-arg constructor is almost unbounded (`Integer.MAX_VALUE`). Use `ArrayBlockingQueue` when the size limit is the point, as it is here with 5 slots. Use `SynchronousQueue` when you want no stored item: each `put` waits for a matching `take`.

Pick the size on purpose. Five slots make the wait easy to see in the console. A size of one makes the two sides take strict turns. A size in the thousands hides the wait and holds a large array the whole time.

## What to use

| You need | Use this |
| --- | --- |
| To show you can write `wait` and `notify` | `ConsumerProducerTest` |
| A queue with a size limit between threads | `ArrayBlockingQueue` |
| A large queue, with producers and consumers busy at the same time | `LinkedBlockingQueue` with a capacity |
| A direct handoff, with no stored item | `SynchronousQueue` |
| The same rules, written out on a `ReentrantLock` | The sketch in section 11 of `01_consumer_producer.md` |

For the numbers in `ConsumerProducerViaArrayBlockingQueueTest`, the call to keep is `put` and `take` on `new ArrayBlockingQueue<>(5)`.

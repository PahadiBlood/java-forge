# Producer-consumer with ArrayBlockingQueue

*Why the bounded queue in the JDK replaces the hand-rolled `wait` / `notify` queue*

Exercise file: `xyz.manojraw.exercises.interview.threads.ConsumerProducerViaArrayBlockingQueueTest`.

The hand-rolled version is `ConsumerProducerTest`, written up in `01_consumer_producer.md`. Read that one first. This note is what you keep once that version is clear.

Checked against the JDK in this repo: OpenJDK 25. `ArrayBlockingQueue` has had this shape since it shipped in `java.util.concurrent`.


## 1. What the demo does

One producer thread calls `put` with `0..999`, then repeats. One consumer thread calls `take`, prints the value, and sleeps one second. The queue holds 5 integers.

The consumer is the slow side, because it sleeps for a second after each `take`. On a run here the log started `Added num 0`, `Take num 0`, then `Added num 1` through `Added num 5` while that sleep held the consumer. Those five values filled the array, so the next `put` parked until `Take num 1` freed a slot. After that the lines alternate, one take and one add. An empty queue parks `take` the same way, so the consumer never spins and never reads a value that was not `put`.

`main` submits both tasks to a pool of two threads and returns. Pool threads are non-daemon, so the process stays up. There is no shutdown. Stop it by hand, the same way as the coffee-shop demo.


## 2. What you had to write by hand

`ConsumerProducerTest` shares an `ArrayDeque` and uses that same object as the monitor:

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

That is correct for one consumer and a queue that is allowed to grow. Every bug in `01_consumer_producer.md` is a way to get those few lines wrong: waiting on a different object, `if` around `wait`, sleeping while the monitor is held, `notify` with several waiters, the consumer method returning, the drain sitting inside the wait condition.

A bounded hand-rolled buffer adds a second condition. This is the shape `ArrayBlockingQueue.put` and `take` already run, with `notFull` and `notEmpty` as the two conditions:

```java
// producer
lock.lockInterruptibly();
try {
    while (count == capacity) {
        notFull.await();          // park while the array is full; the lock is released
    }
    items[putIndex] = value;
    notEmpty.signal();            // one taker can proceed
} finally {
    lock.unlock();
}

// consumer
lock.lockInterruptibly();
try {
    while (count == 0) {
        notEmpty.await();
    }
    value = items[takeIndex];
    notFull.signal();             // one putter can proceed
} finally {
    lock.unlock();
}
```

`signal` wakes one waiter on that condition. A full queue wakes a putter. An empty queue wakes a taker. The `while` runs again after every wake, so a spurious wake or a stolen slot sends the thread back to `await`. The lock is released for the whole wait, and `finally` releases it on the way out. Application code that calls `put` and `take` does not see any of this.


## 3. Why this is the one to keep

**The bound is the queue.** `new ArrayBlockingQueue<>(5)` allocates an array of 5 slots. `put` parks while `count == 5`. The coffee-shop `ArrayDeque` has no capacity. Section 10 of the wait/notify note calls that out: a faster producer grows the deque, and one virtual thread starts per order. Five slots means the sixth order waits. The heap stays at one small array.

**The monitor rules are inside the class.** `put` and `take` take the same `ReentrantLock`, await on a condition, and signal the other condition. Callers cannot wait on the wrong object, cannot use `if` where a `while` belongs, and cannot forget the signal. Those are the defects section 3 and section 8 of the wait/notify note were spent on.

**The lock is not held across your work.** `take` returns, then `Thread.sleep(1000)` runs. The queue lock was released in `take`'s `finally` before the sleep starts, so the producer can `put` during that second. Sleeping inside `synchronized (orders)` was the delay in the coffee-shop version: `wait` releases the monitor, `sleep` does not.

**Several producers and several consumers work on the same queue.** Each `put` signals one taker. Each `take` signals one putter. The single `orders.notify()` in the coffee shop is right for one consumer. Add a second consumer, or a producer that also waits because the queue is full, and one `notify` can wake the wrong side. The two conditions keep those waits apart.

**The value published by `put` is visible to the thread that `take`s it.** The unlock at the end of `put` happens-before the lock at the start of the matching `take`. A hand-rolled queue has that guarantee only while every read and write of the deque stays inside `synchronized (orders)`. A read that forgets the block can observe a stale or torn queue. `ArrayDeque` itself has no internal lock.

**`put(null)` fails immediately.** `ArrayBlockingQueue` rejects null. An `ArrayDeque` accepts null, and a consumer cannot tell "empty" from "a null element" if the protocol uses null as a sentinel.

**Interrupt and timeout are part of the method.** `put` and `take` wait with `lockInterruptibly` and `await`, so an interrupt throws `InterruptedException` and clears the flag. The demo puts the flag back with `Thread.currentThread().interrupt()` and returns, which is the same restore the coffee-shop note settled on. `offer` and `poll` add a timeout when a caller should give up instead of parking. The hand-rolled loop has to build that from `wait(millis)` or `Condition.await(timeout, unit)`.

**Fairness is a constructor flag.** `new ArrayBlockingQueue<>(5)` allows barging: a thread that arrives at the lock can skip a thread already parked on the condition. `new ArrayBlockingQueue<>(5, true)` queues the waiters. Fair mode costs more lock traffic. The demo uses the default. A `synchronized` block has no fair switch.


## 4. What it does not replace

Write the `wait` / `notify` version when the exercise is the monitor. `ConsumerProducerTest` is that exercise. `ArrayBlockingQueue` hides the monitor so you can spend the code on the work.

The coffee shop also starts a virtual thread per order. The queue only hands the task over. `ArrayBlockingQueue` does not start threads and does not brew. A matching coffee shop would `put` the brew task into a queue of capacity 5, and the consumer would `take` and then `startVirtualThread`. The cap applies to the backlog, which is the limit section 10 asked for.

The pool in this demo is never shut down, and both loops run until the thread is interrupted. A real stop is `shutdownNow` on the executor (that interrupts the workers) or a poison pill the consumer understands. The queue will not invent that for you.

`ArrayBlockingQueue` uses one lock for both ends. `LinkedBlockingQueue` splits put and take onto two locks, which overlaps a producer and a consumer more, and its no-arg constructor is effectively unbounded (`Integer.MAX_VALUE`). Use `ArrayBlockingQueue` when the bound is the point, as it is here with 5 slots. Use `LinkedBlockingQueue` with an explicit capacity when the buffer is large and both ends stay busy. `SynchronousQueue` has no slots: each `put` waits for a matching `take`.

Pick the capacity on purpose. Five slots make the block easy to see in the console. A capacity of one serialises the two sides. A capacity in the thousands hides the block and holds a large array the whole time.


## 5. What to reach for

| You need | Reach for |
| --- | --- |
| To show you can write `wait` / `notify` | `ConsumerProducerTest` |
| A bounded buffer between threads | `ArrayBlockingQueue` |
| A large buffer, producers and consumers busy at once | `LinkedBlockingQueue` with a capacity |
| A direct handoff, no stored element | `SynchronousQueue` |
| The same rules, written out on a `ReentrantLock` | The sketch in section 9 of `01_consumer_producer.md` |

For the numbers in `ConsumerProducerViaArrayBlockingQueueTest`, the call to keep is `put` / `take` on `new ArrayBlockingQueue<>(5)`.

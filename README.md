# Java Forge

This project is for learning Core Java.

We do not start with the best answer. We start with the old way. We see the problem. Then we fix it in another way. Then we improve that fix.

## Read the notes

Every topic has a `notes` folder next to the code. Read the notes with the code.

Each step in a note says two things:

- **Issue:** what goes wrong.
- **Fix:** how the next way solves it.

The code shows the steps. The notes tell you why.

## What is in this project

There is one project folder: `java-core`.

The lessons are here:

`java-core/src/test/java/xyz/manojraw/exercises/interview/`

| Folder | What you learn |
| --- | --- |
| `immutable` | An object that should not change after it is created |
| `singleton` | Only one shared object |
| `threads` | One side adds work. The other side does the work |
| `java8plusfeatures` | A very small record example |

`java-core/src/test/resources/` has `cars.json` and `people.json`. The lessons do not use those files.

The Java version is 21. You can run each lesson from its `main` method.

## immutable

**Issue:** A class can look frozen. A later `add` on a list can still change the object. The caller may still hold that list. The getter may give the real list back. `clone` may share the list. A child class may add a setter. An item inside the list may have a setter.

**Fix:** Each next class closes one of those holes. The record file walks the same holes. A record is already a final class. It still stores your list until you copy the list.

| Step | What is wrong | Class that shows the next fix | Record |
| --- | --- | --- | --- |
| 1 | The constructor keeps the caller's list | `SharesCallerList` | `SharesCallerList` |
| 2 | Copy the list in the constructor. The getter still returns the real list | `CopiesListInConstructor` | `CopiesListInConstructor` |
| 3 | The getter hides the list. `clone` still shares it | `UnmodifiableGetterShallowClone` | `UnmodifiableAccessorShallowClone` |
| 4 | `clone` is blocked. The class can still be extended | `CloneDisabledNotFinal` | `CopiedIntegerList` |
| 5 | The class is final and the list rejects `add` | `FinalIntegerList` | A record is already final |
| 6 | Items inside the list can still change. Copy their values | `DeepCopiedMarkers` | `DeepCopiedMarkers` |

Read the notes in this order:

1. [Class note](java-core/src/test/java/xyz/manojraw/exercises/interview/immutable/notes/01_immutable_via_class.md)
2. [Record note](java-core/src/test/java/xyz/manojraw/exercises/interview/immutable/notes/02_immutable_via_record.md)

Run `Main` in the `immutable` folder. It runs the class examples, then the record examples.

Keep `CopiedIntegerList` when the values are `int`, `String`, or `Integer`. Keep `DeepCopiedMarkers` when an item still has a setter. Use `FinalIntegerList` when you need a normal class.

## singleton

**Issue:** You want one shared object. Two threads, reflection, save-and-load, or `clone` can still create a second one.

**Fix:** The folder tries the old ways first, then the better ways.

1. `SingletonExampleEager` — create it when the class loads.
2. `SingletonExampleLazyViaSynchronizedMethod` — create it on the first call. Every call takes a lock.
3. `SingletonExampleLazyViaDoubleLocking` — take the lock only while the object is missing.
4. `SingletonExampleLazyViaHolder` — create it on the first call. Later calls take no lock.
5. `SingletonExampleLazyViaEnum` — the one to keep. The JVM blocks a second object from reflection, `clone`, and save-and-load.

Read [the singleton note](java-core/src/test/java/xyz/manojraw/exercises/interview/singleton/notes/01_how_to_create_a_singleton.md).

`SingletonThreadTest` starts 8 threads on each form. It counts how many different objects they saw. The note says what that test does not catch.

## threads

**Issue:** A coffee shop has two sides. One side adds orders. The other side makes them. If you write the waiting yourself, the consumer can stop, miss a wake-up, or hold the lock too long.

**Fix:** `ConsumerProducerTest` is the hand-written queue. It uses `synchronized`, `wait`, and `notify`. The first note lists each bug and the fix.

**Better fix:** `ConsumerProducerViaArrayBlockingQueueTest` uses `ArrayBlockingQueue`. `put` waits when the queue is full. `take` waits when the queue is empty. You do not write `wait` and `notify` yourself.

Read the notes in this order:

1. [Hand-written queue](java-core/src/test/java/xyz/manojraw/exercises/interview/threads/notes/01_consumer_producer.md)
2. [ArrayBlockingQueue](java-core/src/test/java/xyz/manojraw/exercises/interview/threads/notes/02_array_blocking_queue.md)

Both `main` methods keep running. Stop them by hand.

## java8plusfeatures

`RecordTest` is a short record. It copies itself with `clone`.

The full record lesson is in the immutable folder. Read [the record note](java-core/src/test/java/xyz/manojraw/exercises/interview/immutable/notes/02_immutable_via_record.md).

# Java Forge

Core Java interview topics, learned one problem at a time.

Each topic starts with the old approach and the issue that approach has. The next example fixes that issue in a different way. A later example improves the fix. The code keeps every step so you can see why the next one exists.

Read the `notes` folder beside the code. The notes are how you understand the topic: what used to go wrong, which fix closed it, what that fix still left open, and which version to keep.

## How a topic is built

1. Write the old approach and let the issue show up.
2. Fix that issue with another approach.
3. Improve the fix until you reach the version worth keeping.
4. Read the notes in that topic before you treat the last version as the answer.

Each next example closes the previous leak and leaves the next one open.

## Layout

```
java-core/                                          Maven module, Java 21
└── src/test/
    ├── java/xyz/manojraw/exercises/interview/
    │   ├── immutable/                              class, then record
    │   │   └── notes/
    │   ├── java8plusfeatures/                      small record example
    │   ├── singleton/                              eager, then lazy forms, then enum
    │   │   └── notes/
    │   └── threads/                                wait/notify, then ArrayBlockingQueue
    │       └── notes/
    └── resources/                                  cars.json, people.json
```

Build from `java-core` with Maven. Source and target are Java 21. The module also has JUnit 5, AssertJ, Guava, Gson, and Commons IO. The lessons are plain Java. Run a topic by running its `main`.

`resources` still has `cars.json` and `people.json`. The interview examples do not read those files.

## immutable

Make an object that callers cannot change after the constructor.

The first type stores the caller's list, so a later `add` changes the object. The next type copies the list in the constructor, and the getter still returns a live list. Later steps wrap that list, stop `clone` from sharing it, close the class to subclasses, then copy mutable elements into values that have no setter. The record file walks the same leaks. A record is already final, and it still stores a list reference until you copy the list.

| Step | Class | Record |
| --- | --- | --- |
| Caller's list is stored | `SharesCallerList` | `SharesCallerList` |
| Copy in the constructor | `CopiesListInConstructor` | `CopiesListInConstructor` |
| Getter or accessor no longer returns a live list | `UnmodifiableGetterShallowClone` | `UnmodifiableAccessorShallowClone` |
| `clone` no longer shares the list | `CloneDisabledNotFinal` | `CopiedIntegerList` |
| Class cannot be subclassed, list is copied | `FinalIntegerList` | already final; `CopiedIntegerList` |
| Mutable elements are copied as values | `DeepCopiedMarkers` | `DeepCopiedMarkers` |

Notes, in this order:

- [`immutable/notes/01_immutable_via_class.md`](java-core/src/test/java/xyz/manojraw/exercises/interview/immutable/notes/01_immutable_via_class.md) — each class, the leak it closes, and the leak it leaves.
- [`immutable/notes/02_immutable_via_record.md`](java-core/src/test/java/xyz/manojraw/exercises/interview/immutable/notes/02_immutable_via_record.md) — the same walk on a record, and why a record does not freeze a `List` by itself.

Run `xyz.manojraw.exercises.interview.immutable.Main`. It runs every class demo, then every record demo.

Keep `CopiedIntegerList` when the fields are already immutable (`int`, `String`, `Integer`). Keep `DeepCopiedMarkers` when an element still has a setter. Use `FinalIntegerList` when the type has to be a normal class.

## singleton

One shared instance, then the ways a second instance still appears.

The folder moves from the older forms to the one to keep:

1. `SingletonExampleEager` — created when the class loads.
2. `SingletonExampleLazyViaSynchronizedMethod` — created on first use. Every call takes the lock.
3. `SingletonExampleLazyViaDoubleLocking` — the lock is taken only while the instance is missing.
4. `SingletonExampleLazyViaHolder` — lazy class. Later reads take no lock.
5. `SingletonExampleLazyViaEnum` — the form to keep. The JVM blocks a second object from reflection, `clone`, and serialization.

Notes:

- [`singleton/notes/01_how_to_create_a_singleton.md`](java-core/src/test/java/xyz/manojraw/exercises/interview/singleton/notes/01_how_to_create_a_singleton.md) — what each form fixes, which form to prefer, and the attacks that still make a second object for a normal class.

`SingletonThreadTest` starts several threads on each form and counts how many distinct objects they saw. The note explains what that run shows and what it leaves out (reflection, save-and-load, `clone`, a second class loader).

## threads

A producer puts work on a shared queue. A consumer takes that work off. They do not call each other.

`ConsumerProducerTest` is the old approach: your own queue, `synchronized`, `wait`, and `notify`. The first note is the issues that showed up in that code and the fix for each one. The consumer could stop after a single drain. `wait` outside a loop could miss a signal. Work done while holding the lock blocked the other side.

`ConsumerProducerViaArrayBlockingQueueTest` is the improved version. `ArrayBlockingQueue` blocks `put` when the queue is full and `take` when it is empty, so that hand-written waiting goes away.

Notes, in this order:

1. [`threads/notes/01_consumer_producer.md`](java-core/src/test/java/xyz/manojraw/exercises/interview/threads/notes/01_consumer_producer.md) — the hand-written queue, the bugs, and how each bug was fixed.
2. [`threads/notes/02_array_blocking_queue.md`](java-core/src/test/java/xyz/manojraw/exercises/interview/threads/notes/02_array_blocking_queue.md) — why the library queue is the one to keep, and what it still does not do for you.

Both `main` methods keep running. Stop them by hand.

## java8plusfeatures

`RecordTest` is a short record that implements `Cloneable` and clones itself. The full record lesson, including what `clone` still shares, is the immutable folder and [`02_immutable_via_record.md`](java-core/src/test/java/xyz/manojraw/exercises/interview/immutable/notes/02_immutable_via_record.md).

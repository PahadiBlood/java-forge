# How do you create a singleton object in Java?

A singleton is one instance of a type, shared by every caller in the same class loader. Callers get it from a public access point (`getInstance()` or an enum constant). They do not call `new`.

These notes walk the five forms in this folder, then the attacks that still produce a second object, then which form to reach for.

Checked on the JDK in this repo: OpenJDK 25.

## What to prioritize

Decide in this order.

1. **Do you need a singleton at all?** One object created in `main` (or by Spring, Guice, and so on) and passed into constructors is easier to test. A singleton is global state: every caller shares it, and a test cannot swap it without reflection or a static reset. Reach for a singleton when the thing truly is one-per-process: a cache, a connection registry, a logger facade.

2. **When you do need one, prefer the enum** — `SingletonExampleLazyViaEnum`. The JVM creates `INSTANCE` once, on first use, under the class-initialization lock. The same JVM rules block a second object from reflection, from `clone()`, and from serialization. This is the form Joshua Bloch argues for in *Effective Java*.

3. **When the type has to be a normal class, prefer the holder** — `SingletonExampleLazyViaHolder`. Use it if you must extend a superclass (`enum` already extends `java.lang.Enum`) or a framework insists on a class. It is lazy and thread-safe, and the read path takes no lock. It is still open to reflection, serialization, and cloning. Those holes are below.

4. **Eager** (`SingletonExampleEager`) when the object is cheap and you are happy to build it as soon as the class loads.

5. **Synchronized `getInstance`** when the call is rare and you want the shortest correct lazy method. The lock is taken on every call.

6. **Double-checked locking** only when you already understand `volatile` and you have a reason the holder does not fit. It is correct in `SingletonExampleLazyViaDoubleLocking`. It has more ways to get the threading wrong than the holder, and it gains nothing over the holder for a plain singleton.

| You need | Reach for | File |
| --- | --- | --- |
| The default singleton | Enum | `SingletonExampleLazyViaEnum` |
| A class, created on first use | Holder | `SingletonExampleLazyViaHolder` |
| A class, created at class load | Eager `static final` | `SingletonExampleEager` |
| Obvious lazy code, few callers | `synchronized` method | `SingletonExampleLazyViaSynchronizedMethod` |
| Lazy class, and you accept the memory-model rules | Double-checked locking | `SingletonExampleLazyViaDoubleLocking` |

`SingletonThreadTest` starts 8 threads on each form and prints how many distinct objects they saw. One distinct identity hash means those threads agreed. That run says nothing about reflection, serialization, or a second class loader.

## What every class form shares

```java
public class Singleton {
    private static Singleton instance;          // one slot, shared

    private Singleton() {                       // callers cannot write new Singleton()
    }

    public static Singleton getInstance() {     // the only door
        return instance;
    }
}
```

Three rules sit under every variant:

- The constructor is private, so ordinary code outside this class cannot call `new`.
- The instance lives in a `static` field, so there is one slot per loaded class.
- `getInstance()` is static, so callers can reach it with no object in hand.

The variants differ in **when** the object is built and **how** they keep two threads from building two of them.

A private constructor stops `new` written in source. It does not stop `java.lang.reflect.Constructor`, serialization, or `clone()`. Those are the same for eager, the synchronized method, double-checked locking, and the holder. The enum section is where they stop.

## 1. Eager: create it when the class loads

```java
public class SingletonExampleEager {
    private static final SingletonExampleEager instance = new SingletonExampleEager();

    private SingletonExampleEager() {
    }

    public static SingletonExampleEager getInstance() {
        return instance;
    }
}
```

The JVM initializes a class under its own lock, on one thread, before any thread is allowed to use that class. The `static final` field is assigned during that initialization. After it finishes, every thread sees `instance` already set. No `synchronized`, no `volatile`.

`final` stops ordinary code from writing `instance = new SingletonExampleEager()` later. Delete `final` and a method on this class can point the field at a new object. That is a second instance, and `getInstance()` will hand it out. `final` does not stop the attacks in the second half of these notes.

**Drawback — you pay even when nobody calls `getInstance()`.** Touching the class for any other static member runs this constructor. A heavy constructor (file, network, big cache) runs at class-load time, including in tests that only wanted a helper on the same class. If the constructor throws, the JVM marks the class as failed and every later use throws `NoClassDefFoundError` / `ExceptionInInitializerError` for the life of that class loader.

**Drawback — startup order.** The constructor runs inside class initialization. If it calls another class whose initialization calls back into this one, you can deadlock on the two class-init locks, or read a field that is still at its default value.

Use eager when construction is cheap and the process always needs the object.

## 2. Lazy: synchronized method

```java
public class SingletonExampleLazyViaSynchronizedMethod {
    private static SingletonExampleLazyViaSynchronizedMethod s;

    private SingletonExampleLazyViaSynchronizedMethod() {
    }

    public static synchronized SingletonExampleLazyViaSynchronizedMethod getInstance() {
        if (s == null) {
            s = new SingletonExampleLazyViaSynchronizedMethod();
        }
        return s;
    }
}
```

`synchronized` on a static method locks the `Class` object. One thread at a time enters `getInstance()`. The first thread sees `s == null` and assigns it. The next thread waits for that lock, then sees `s` already set, because unlocking happens-before the next lock. `volatile` is unnecessary here. The monitor already publishes the write.

The object is created on the first call, so an unused class stays cheap.

**Drawback — the lock is taken on every call.** After the object exists, the method still does nothing but `return s`, and it still acquires the class lock to do it. Callers queue behind each other. That is correct, and it is wasteful when `getInstance()` is on a hot path.

A `Thread.sleep` inside this method makes the queue obvious and is the wrong place to demonstrate it. The sleep holds the class lock for the whole delay, so two callers serialize for two full sleeps, and every later caller pays it too. The cost that belongs to this pattern is the lock itself, not an artificial pause.

Use this form when you want the code to be obvious and the method is called rarely (startup, a CLI command, a test).

## 3. Lazy: double-checked locking

```java
private static volatile SingletonExampleLazyViaDoubleLocking s;

public static SingletonExampleLazyViaDoubleLocking getInstance() {
    if (s == null) {                                      // fast path, no lock
        synchronized (SingletonExampleLazyViaDoubleLocking.class) {
            if (s == null) {                              // re-check under the lock
                s = new SingletonExampleLazyViaDoubleLocking();
            }
        }
    }
    return s;
}
```

The outer `if` keeps the common case off the lock. The inner `if` is what makes it correct: two threads can both see `s == null` before either takes the lock. Only the thread that wins the lock creates the object. The other thread enters the lock afterwards, sees a non-null `s`, and returns it.

`new` is three steps: allocate memory, run the constructor, store the reference in `s`. Without `volatile`, the JVM is allowed to publish the reference before the constructor finishes. Another thread can pass the outer `if`, skip the lock, and return an object whose fields are still default values. `volatile` forbids that reorder and makes the write visible to the other cores. Both checks stay. `volatile` alone, with a single `if`, still lets two threads construct two objects.

The long comment on `SingletonExampleLazyViaDoubleLocking` is this same rule, written next to the field.

**Drawback — it is easy to break and it does not beat the holder.** Drop `volatile`, drop the inner `if`, or lock on a different object than the one every thread synchronizes on, and the singleton is wrong again. The holder gets the same laziness from the class-initialization lock, with no `volatile` to forget. Prefer the holder unless you have a specific reason to keep the field on the outer class.

**Drawback — reflection, serialization, and cloning** still apply. The threading fixes do not close them. The top comment on that file is about reflection, and it is right.

## 4. Lazy: holder class (the class form to prefer)

```java
public class SingletonExampleLazyViaHolder {
    private SingletonExampleLazyViaHolder() {
    }

    private static class Holder {
        private static final SingletonExampleLazyViaHolder INSTANCE =
                new SingletonExampleLazyViaHolder();
    }

    public static SingletonExampleLazyViaHolder getInstance() {
        return Holder.INSTANCE;
    }
}
```

`Holder` is not loaded when `SingletonExampleLazyViaHolder` is loaded. It is loaded the first time `getInstance()` reads `Holder.INSTANCE`. Loading it runs the JVM's class initialization, which assigns `INSTANCE` on one thread and then publishes that value to every thread. Same guarantee as the eager form, delayed until first use.

No lock on the read path after that. No `volatile` on the outer class. The constructor still runs at most once per class loader, as far as ordinary calls go.

**Drawback — it is still a normal class.** A reflective constructor call, a deserialized copy, or `clone()` each produce another object. The holder only solves the threading problem. Enum solves the threading problem and those three.

**Drawback — one instance, no arguments.** The constructor runs from a static field, so there is no place to pass configuration in. If the object needs a host, a port, or a path, build it in `main` and pass it down. Forcing those through static setters is shared mutable state with a singleton's name on it.

## 5. Enum (prioritize this)

```java
public enum SingletonExampleLazyViaEnum {
    INSTANCE
}
```

Callers use `SingletonExampleLazyViaEnum.INSTANCE`.

An enum constant is created when the enum class is initialized, which happens the first time anything touches the enum. With a single constant, that is first use of `INSTANCE`. The JVM does it under the class-init lock, so threads cannot each build their own.

What you get on top of the holder, from the JVM, not from a guard you wrote:

- **Reflection.** The real constructor is `private SingletonExampleLazyViaEnum(String name, int ordinal)`. `Constructor.newInstance(...)` throws `IllegalArgumentException: Cannot reflectively create enum objects`.
- **Serialization.** The stream stores the constant's name. Reading it back resolves that name to the existing `INSTANCE`. A round trip returns the same reference.
- **Cloning.** `Enum.clone()` throws `CloneNotSupportedException`. You cannot override it to return a copy; `clone()` is final on `Enum`.

**Drawback — an enum cannot extend a class.** It can implement interfaces. The moment you need a superclass, switch to the holder and read the attack section. You will be patching holes the enum already closed.

**Drawback — the type has enum API.** Callers see `name()`, `ordinal()`, `values()`, and `compareTo`. That is harmless, and it looks odd on a type that is "really" a service. If that bothers the API, the holder is the next choice, with the holes accepted or patched.

**Drawback — first touch creates it, and a rename breaks old serialized data.** Any use of the enum class initializes every constant. One constant means that is the same as lazy. Serialized form is the constant name, so renaming `INSTANCE` makes old streams fail when they are read.

**Drawback — still one per class loader, not one per JVM.** Two class loaders means two copies of this enum and two `INSTANCE`s. The enum does not fix that. Nothing in this folder does.

## Attacks that produce a second object

These apply to every **class** form above. The enum column is what the JVM does instead.

### Reflection

`setAccessible(true)` turns off the private check. `newInstance()` calls the constructor and returns the object. It does not go through `getInstance()`, so the static field is a different reference.

```java
Constructor<SingletonExampleEager> ctor =
        SingletonExampleEager.class.getDeclaredConstructor();
ctor.setAccessible(true);
SingletonExampleEager extra = ctor.newInstance();

SingletonExampleEager canonical = SingletonExampleEager.getInstance();
// extra != canonical
```

On OpenJDK 25 that pair compares unequal. `getInstance()` keeps returning the original. Anyone holding `extra` is holding a second object. State you mutate on one is invisible on the other. That is the singleton contract broken.

A guard inside the constructor closes the obvious version of this:

```java
private SingletonExampleEager() {
    if (instance != null) {
        throw new IllegalStateException("Use getInstance()");
    }
}
```

For the **eager** form this works against a later reflective `newInstance()`, because class initialization has already assigned `instance`. For the **lazy** forms it is weaker:

- Call the constructor reflectively *before* `getInstance()`. The field is still `null`, so the guard allows it. You now hold an object. `getInstance()` later builds another one and stores that. Two objects.
- The lazy field is not `final`. Reflection can set it back to `null` (`Field.set` on a non-final static field succeeds), then `newInstance()` passes the guard again.

`final` on the eager field does **not** stop `Constructor.newInstance`. It stops a different trick: `Field.set` on a `static final` field. On this JDK that set throws `IllegalAccessException` (`Can not set static final ... to null value`). The attacker does not need to clear the field. They call the constructor and keep the object it returns.

An **instance** `final` field is a third, unrelated case. `Field.set` on a private final instance field such as `String message` succeeds on this JDK and overwrites it. That experiment changes one field of an existing object. It does not create a singleton, and making the field `static` is not what protects `getInstance()`.

`sun.misc.Unsafe.allocateInstance` / `jdk.internal.misc.Unsafe.allocateInstance` allocates an object without running any constructor. A guard written in the constructor never sees that call. Code that can call `Unsafe` is already outside the model these patterns protect. Enum is still the form that closes the ordinary path (`Constructor`, `clone`, serialization) without a hand-written guard.

### Serialization

Implement `Serializable` and the default serialization mechanism builds a new instance from the byte stream. It does not call your constructor. A round trip of the lazy class returns a different reference. The eager class behaves the same way the moment it implements `Serializable`.

The patch is `readResolve()`, which the serialization machinery calls after building the new object. Return the canonical instance and the caller of `readObject` receives that instead:

```java
private Object readResolve() {
    return getInstance();
}
```

Forget `readResolve` and every deserialized copy is a new singleton. Enum serialization does this for you by resolving the constant name.

### Cloning

`Object.clone()` throws `CloneNotSupportedException` unless the class implements `Cloneable`. The moment it does, and `clone()` calls `super.clone()`, the result is a shallow copy: a second object, constructor not called.

```java
public Object clone() throws CloneNotSupportedException {
    return super.clone();   // a new instance, same field values
}
```

Leave `Cloneable` off the type. If a superclass forces you to deal with `clone()`, override it to throw, or return `getInstance()` so the caller still holds the one object. `Enum.clone()` already throws, and it is final.

### A second class loader

Static fields belong to the `Class` object, and each class loader has its own `Class` for the same binary name. An application server with two webapps, or a plugin loader, loads your singleton class twice. Each copy has its own `instance` or its own `INSTANCE`.

Threads inside one loader still agree. Across loaders they do not. Enum, holder, eager, and double-checked locking all behave this way. If you truly need one object per JVM, you have to put that object in a class loaded by the parent loader, not hunt for a cleverer `getInstance()`.

### What `SingletonThreadTest` can and cannot show

The test calls each public access point from 8 threads and counts `System.identityHashCode`. `identityHashCode` follows the reference, so an overridden `hashCode()` cannot make two objects look like one.

A line like `eager distinct instances: 1 (one instance)` means those eight threads observed one object. Run it when you change a form. It will stay green if you then break the class with reflection, because the test never calls `Constructor.newInstance`.

## Patches, in the order they are worth writing

Only for a **class** form. Skip all of this when you used the enum.

1. Private constructor.
2. `static final` field when the form allows it (eager, holder). Stops ordinary reassignment, and on current JDKs stops `Field.set` from clearing that slot.
3. Constructor guard (`if (instance != null) throw`) so a reflective `newInstance()` after initialization fails. Remember the lazy hole: reflection before the field is assigned, or setting a non-final field back to `null`, still gets through.
4. `readResolve()` if the class implements `Serializable`.
5. Do not implement `Cloneable`. Override `clone()` to throw if a parent leaves you no choice.

A class with all five patches is still behind the enum. `Unsafe.allocateInstance` skips the constructor, and a second class loader skips the field. The enum is ahead because you did not have to remember 3, 4, and 5.

## How to choose while reading this folder

- Interview question "write a singleton" → enum. Say why reflection, serialization, and cloning come out as the same instance. Mention the holder if they ask "what if I have to extend a class?"
- You are about to type `synchronized` on `getInstance` → the holder is the same laziness with no lock on the later calls.
- You are about to write double-checked locking → confirm `volatile` is on the field and both null checks are there, then ask why the holder is not enough. The answer is usually "it is enough."
- You are about to add `Serializable` or `Cloneable` to a class singleton → add `readResolve`, or switch the type to an enum before those interfaces go on.
- The object needs constructor arguments → it is not a singleton pattern problem. Create it once at the top of the program and pass it in.

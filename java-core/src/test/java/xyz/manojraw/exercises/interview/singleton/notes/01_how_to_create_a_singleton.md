# How do you create a singleton in Java?

A singleton means one object of a type, shared by everyone in the same class loader.

Callers do not write `new`. They call `getInstance()`, or they use an enum constant.

This note walks the five files in this folder. Each one fixes a problem from the one before it. Then it shows the attacks that can still make a second object.

These checks were run on OpenJDK 25.

## Which one should you use?

Ask in this order.

1. Do you need a singleton at all? One object created in `main` and passed into constructors is easier to test. A singleton is shared by everyone. A test cannot swap it without reflection or a static reset. Use a singleton when there really is one per program, such as a cache, a connection list, or a logger.
2. If you need one, prefer the enum: `SingletonExampleLazyViaEnum`. The JVM creates `INSTANCE` once, on first use. The same JVM rules block a second object from reflection, from `clone()`, and from save-and-load.
3. If the type must be a normal class, prefer the holder: `SingletonExampleLazyViaHolder`. Use it when you must extend a class. An enum already extends `Enum`, so it cannot extend your class. The holder is lazy and safe for threads. Later reads take no lock. Reflection, save-and-load, and `clone` can still make a second object.
4. Use eager (`SingletonExampleEager`) when the object is cheap and you are fine creating it as soon as the class loads.
5. Use a synchronized `getInstance` when calls are rare and you want the shortest lazy method. The lock is taken on every call.
6. Use double-checked locking only when you already understand `volatile`, and the holder does not fit. The file here is correct. It is easier to get wrong than the holder. For a plain singleton it does not beat the holder.

| You need | Use this | File |
| --- | --- | --- |
| The normal choice | Enum | `SingletonExampleLazyViaEnum` |
| A class, created on first use | Holder | `SingletonExampleLazyViaHolder` |
| A class, created when the class loads | Eager | `SingletonExampleEager` |
| Simple lazy code, few calls | `synchronized` method | `SingletonExampleLazyViaSynchronizedMethod` |
| A lazy class, and you accept the extra rules | Double-checked locking | `SingletonExampleLazyViaDoubleLocking` |

`SingletonThreadTest` starts 8 threads on each form. It prints how many different objects they saw. One object means those threads agreed. That run says nothing about reflection, save-and-load, or a second class loader.

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

Three rules sit under every class form:

- The constructor is private. Normal code outside this class cannot call `new`.
- The object lives in a `static` field. There is one slot for each loaded class.
- `getInstance()` is static. Callers can use it without an object in hand.

The forms differ in two ways: when the object is built, and how they stop two threads from building two objects.

A private constructor stops `new` in source code. It does not stop reflection, save-and-load, or `clone()`. That is true for eager, the synchronized method, double-checked locking, and the holder. The enum is where those stop.

## 1. Eager: create it when the class loads

**Issue:** If two threads both run `new`, you can get two objects. You also want the object ready as soon as the class is used.

**Fix:**

```java
private static final SingletonExampleEager instance = new SingletonExampleEager();

public static SingletonExampleEager getInstance() {
    return instance;
}
```

The JVM sets up a class on one thread, behind its own lock, before any thread may use that class. The `static final` field is set during that work. After it finishes, every thread sees `instance` already set. You do not need `synchronized` or `volatile`.

`final` stops normal code from writing `instance = something else` later. If you delete `final`, a method in this class can point the field at a new object. Then `getInstance()` hands out that second object. `final` does not stop the attacks later in this note.

**Issue still open:** You pay even when nobody calls `getInstance()`. Any use of the class runs this constructor. A heavy constructor (a file, the network, a big cache) runs at class-load time. That includes a test that only wanted some other helper on the same class. If the constructor throws, the JVM marks the class as failed. Later use throws `NoClassDefFoundError` or `ExceptionInInitializerError` for the life of that class loader.

**Another issue:** The constructor runs while the class is starting. If it calls another class, and that class calls back into this one, the two class-start locks can deadlock. Or you can read a field that is still at its default value.

Use eager when building the object is cheap, and the program always needs it.

## 2. Lazy: synchronized method

**Issue:** The eager form builds the object even if nobody asks for it.

**Fix:** Build it on the first call. `synchronized` on a static method locks the `Class` object. One thread at a time enters `getInstance()`.

```java
public static synchronized SingletonExampleLazyViaSynchronizedMethod getInstance() {
    if (s == null) {
        s = new SingletonExampleLazyViaSynchronizedMethod();
    }
    return s;
}
```

The first thread sees `s == null` and sets it. The next thread waits for the lock, then sees `s` already set. The lock makes that write visible. You do not need `volatile` here.

An unused class stays cheap.

**Issue still open:** The lock is taken on every call. After the object exists, the method only does `return s`, but it still takes the class lock. Callers wait in line. That is correct. It is wasteful when `getInstance()` is called all the time.

Do not put `Thread.sleep` inside this method to "show" the wait. The sleep holds the class lock for the whole delay. The real cost of this pattern is the lock itself.

Use this form when you want the code to be obvious, and the method is called rarely.

## 3. Lazy: double-checked locking

**Issue:** The synchronized method takes a lock on every call, even after the object exists.

**Fix:** Check once without a lock. Only the first threads enter the lock. Check again inside the lock, because two threads can both see `s == null` before either takes the lock. Only the thread that wins the lock creates the object. The other thread enters later, sees `s` already set, and returns it.

```java
private static volatile SingletonExampleLazyViaDoubleLocking s;

public static SingletonExampleLazyViaDoubleLocking getInstance() {
    if (s == null) {
        synchronized (SingletonExampleLazyViaDoubleLocking.class) {
            if (s == null) {
                s = new SingletonExampleLazyViaDoubleLocking();
            }
        }
    }
    return s;
}
```

`new` is three steps: reserve memory, run the constructor, store the reference in `s`. Without `volatile`, Java may store the reference before the constructor finishes. Another thread can skip the lock and return an object whose fields are still default values. `volatile` stops that reorder, and it makes the write visible to other cores.

You still need both `if` checks. `volatile` alone, with one `if`, still lets two threads build two objects.

**Issue still open:** This is easy to break, and it does not beat the holder. Drop `volatile`, drop the inner `if`, or lock on a different object, and the singleton is wrong again. The holder gets the same laziness from class startup, with no `volatile` to forget. Prefer the holder unless you have a real reason to keep the field on the outer class.

Reflection, save-and-load, and `clone` still apply. The threading fix does not close them.

## 4. Lazy: holder class

**Issue:** Double-checked locking is easy to write wrong. You want a lazy class with no lock on later reads.

**Fix:** Put the object in a nested class. `Holder` is not loaded when the outer class is loaded. It is loaded the first time `getInstance()` reads `Holder.INSTANCE`.

```java
private static class Holder {
    private static final SingletonExampleLazyViaHolder INSTANCE =
            new SingletonExampleLazyViaHolder();
}

public static SingletonExampleLazyViaHolder getInstance() {
    return Holder.INSTANCE;
}
```

Loading `Holder` uses the JVM's class startup. It assigns `INSTANCE` on one thread, then every thread can see it. This is the same promise as the eager form, but it waits until the first call.

After that, the read takes no lock. The outer class has no `volatile`. For normal calls, the constructor runs at most once per class loader.

This is the class form to prefer.

**Issue still open:** It is still a normal class. A reflective constructor call, a saved-and-loaded copy, or `clone()` can each make another object. The holder only solves the thread problem. The enum solves the thread problem and those three.

**Another issue:** The constructor runs from a static field, so you cannot pass arguments. If the object needs a host, a port, or a path, build it in `main` and pass it down. Static setters for that config are shared mutable state with a singleton name on it.

## 5. Enum: the one to keep

**Issue:** Every class form above can still be broken by reflection, save-and-load, or `clone`.

**Fix:**

```java
public enum SingletonExampleLazyViaEnum {
    INSTANCE
}
```

Callers use `SingletonExampleLazyViaEnum.INSTANCE`.

An enum constant is created when the enum class starts. That happens the first time anything touches the enum. With one constant, that is the first use of `INSTANCE`. The JVM does it under the class-start lock, so threads cannot each build their own.

You get this from the JVM, not from a check you wrote:

- **Reflection.** `Constructor.newInstance(...)` throws `IllegalArgumentException: Cannot reflectively create enum objects`.
- **Save-and-load.** The saved data stores the constant's name. Reading it back finds the existing `INSTANCE`. You get the same object.
- **Clone.** `Enum.clone()` throws `CloneNotSupportedException`. You cannot override it. `clone()` is final on `Enum`.

**Issue still open:** An enum cannot extend a class. It can implement interfaces. If you need a superclass, use the holder and read the attack section. You will be patching holes the enum already closed.

**Another issue:** Callers see enum methods: `name()`, `ordinal()`, `values()`, and `compareTo`. That is harmless. It looks odd on a type that is really a service. If that bothers the API, the holder is the next choice.

**Another issue:** The first touch creates it. Any use of the enum class creates every constant. One constant means that is the same as lazy. Saved data stores the constant name, so renaming `INSTANCE` makes old saved data fail when it is read.

**Another issue:** You still get one object per class loader, not one per JVM. Two class loaders means two copies of this enum and two `INSTANCE` values. Nothing in this folder fixes that.

## Attacks that make a second object

These attacks work on every **class** form above. The enum is what the JVM does instead.

### Reflection

**Issue:** `setAccessible(true)` turns off the private check. `newInstance()` calls the constructor. It does not go through `getInstance()`, so you get a different object.

```java
Constructor<SingletonExampleEager> ctor =
        SingletonExampleEager.class.getDeclaredConstructor();
ctor.setAccessible(true);
SingletonExampleEager extra = ctor.newInstance();
// extra != SingletonExampleEager.getInstance()
```

On OpenJDK 25 those two are not the same object. `getInstance()` still returns the original. Anyone holding `extra` holds a second object. A change on one is invisible on the other.

**Fix for the obvious case:** Throw if the object already exists.

```java
private SingletonExampleEager() {
    if (instance != null) {
        throw new IllegalStateException("Use getInstance()");
    }
}
```

For the **eager** form, this works against a later reflective `newInstance()`, because class startup has already set `instance`.

For the **lazy** forms, the guard is weaker:

- Call the constructor with reflection *before* `getInstance()`. The field is still `null`, so the guard allows it. You now hold an object. `getInstance()` later builds another one. Two objects.
- The lazy field is not `final`. Reflection can set it back to `null`. Then `newInstance()` passes the guard again.

`final` on the eager field does not stop `Constructor.newInstance`. It stops a different trick: `Field.set` on a `static final` field. On this JDK that set throws `IllegalAccessException`. The attacker does not need to clear the field. They call the constructor and keep the object it returns.

An instance `final` field is a different case. `Field.set` on a private final instance field, such as `String message`, succeeds on this JDK and overwrites it. That changes one field of an existing object. It does not create a second singleton.

`Unsafe.allocateInstance` creates an object without running any constructor. A guard in the constructor never sees that call. Code that can call `Unsafe` is already outside what these patterns protect.

**The stronger fix:** Use the enum. It closes the normal path (`Constructor`, `clone`, save-and-load) without a hand-written guard.

### Save and load

**Issue:** If the class implements `Serializable`, the default load builds a new object from the bytes. It does not call your constructor. A round trip returns a different object. The eager class does the same thing as soon as it implements `Serializable`.

**Fix:** Add `readResolve()`. Java calls it after it builds the new object. Return the real instance, and the caller receives that instead:

```java
private Object readResolve() {
    return getInstance();
}
```

If you forget `readResolve`, every loaded copy is a new singleton. Enum save-and-load does this for you by finding the constant name.

### Clone

**Issue:** `Object.clone()` throws unless the class implements `Cloneable`. Once it does, and `clone()` calls `super.clone()`, the result is a second object. The constructor is not called.

```java
public Object clone() throws CloneNotSupportedException {
    return super.clone();   // a new object, same field values
}
```

**Fix:** Do not implement `Cloneable`. If a parent class forces you to deal with `clone()`, override it to throw, or return `getInstance()` so the caller still holds the one object. `Enum.clone()` already throws, and it is final.

### A second class loader

**Issue:** Static fields belong to the `Class` object. Each class loader has its own `Class` for the same name. Two web apps, or a plugin loader, can load your class twice. Each copy has its own `instance` or its own `INSTANCE`.

**Fix:** Threads inside one loader still agree. Across loaders they do not. Enum, holder, eager, and double-checked locking all work this way. If you truly need one object per JVM, put that class in the parent class loader. A cleverer `getInstance()` does not fix this.

### What SingletonThreadTest can and cannot show

The test calls each public door from 8 threads. It counts `System.identityHashCode`. That number follows the object itself, so an overridden `hashCode()` cannot make two objects look like one.

A line like `eager distinct instances: 1` means those eight threads saw one object. Run it when you change a form. It will still say 1 if you then break the class with reflection, because the test never calls `Constructor.newInstance`.

## Patches, in the order they are worth writing

Only for a **class** form. Skip all of this when you used the enum.

1. Private constructor.
2. A `static final` field when the form allows it (eager, holder). This stops normal reassignment. On current JDKs it also stops `Field.set` from clearing that slot.
3. A constructor guard (`if (instance != null) throw`) so a reflective `newInstance()` after startup fails. Remember the lazy hole: reflection before the field is set, or setting a non-final field back to `null`, still gets through.
4. `readResolve()` if the class implements `Serializable`.
5. Do not implement `Cloneable`. Override `clone()` to throw if a parent leaves you no choice.

A class with all five patches is still behind the enum. `Unsafe.allocateInstance` skips the constructor. A second class loader skips the field. The enum is ahead because you did not have to remember steps 3, 4, and 5.

## How to choose while you read this folder

- The interview question is "write a singleton". Use the enum. Say why reflection, save-and-load, and `clone` still return the same object. Mention the holder if they ask "what if I must extend a class?"
- You are about to type `synchronized` on `getInstance`. The holder is the same laziness, with no lock on the later calls.
- You are about to write double-checked locking. Check that `volatile` is on the field and both null checks are there. Then ask why the holder is not enough. The answer is usually "it is enough."
- You are about to add `Serializable` or `Cloneable` to a class singleton. Add `readResolve`, or switch the type to an enum before those interfaces go on.
- The object needs constructor arguments. This is not a singleton problem. Create it once at the top of the program and pass it in.

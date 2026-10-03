# How do you make a class immutable in Java?

An immutable object publishes its state once, in the constructor. After that, normal callers can read the state and cannot change it. `int` and `String` already work that way. A `List` does not: the variable can point at a list the caller still holds, and the list itself accepts `add`.

`ImmutableViaClassTest` walks that in order. Each nested type closes the leak from the previous type and leaves the next leak open. `immutable.Main` calls every `demo`, then the record chain in `ImmutableViaRecordTest`.

Checked on the JDK in this repo: OpenJDK 25.0.2.

## What to prioritize

1. **Prefer a record when every component is already immutable.** `ImmutableViaRecordTest.CopiedIntegerList` is `int`, `String`, and `List<Integer>`. `Integer` has no setter. The compact constructor does `uniqueNumbers = List.copyOf(uniqueNumbers)`. The record is final, the field is private, and `uniqueNumbers()` returns that immutable list. Caller `add` stays on the caller's list. `add` on the accessor throws `UnsupportedOperationException`. `List.copyOf` also rejects a null list and a null element. The record walk is `notes/02_immutable_via_record.md`.

2. **When a component is mutable, copy the values, then store an immutable list.** `SharesMutableElements` is final and uses `List.copyOf`, and `setLabel` on a `Marker` still changes the object, because `List.copyOf` copies the list and keeps the same element objects. `DeepCopiedMarkers` reads each label into a `String` at construction time and stores `List.copyOf` of those strings. Later `setLabel` writes the caller's `Marker` only.

3. **Use a final class when a record does not fit** (a framework wants a particular superclass, or you want named getters). `FinalIntegerList` is that shape: `final` class, `private final` fields, `List.copyOf` in the constructor, public `clone()` that throws `CloneNotSupportedException`. Build a second instance with the constructor. `new FinalIntegerList(existing.getId(), existing.getName(), existing.getUniqueNumbers())` is safe, because the constructor copies again.

4. **Stop at `List.copyOf` stored in the field.** Return that field from the getter. `Collections.unmodifiableList` is a view over some other list. It is the right intermediate fix in `UnmodifiableGetterShallowClone`. It is the wrong place to stop, because anything that still holds the backing `ArrayList` updates every view.

| Situation | What still changes the object | Type that closes it |
| --- | --- | --- |
| Constructor stores the caller's list | `input.add(...)` | `CopiesListInConstructor` |
| Getter returns the internal `ArrayList` | `getUniqueNumbers().add(...)` | `UnmodifiableGetterShallowClone` |
| `clone()` copies the list reference | a write to that shared `ArrayList` shows on both objects | `CloneDisabledNotFinal` |
| Class is open | a subclass adds a setter | `FinalIntegerList` |
| Element type has a setter | `marker.setLabel(...)` | `DeepCopiedMarkers` |
| Same-module `Field.set` on the instance | replaces the private final field | no class in this file; see the ceiling below |

## Where the old `main` went

The previous `main` ran three writes against one object:

- `a.add(34)` after `new` changes the object when the constructor stores `a`. That is `SharesCallerList`.
- `i.getUniqueNumbers().add(58)` changes the object when the getter returns the internal list. That is `CopiesListInConstructor`. The constructor copy is already in place, so `a.add(34)` no longer shows up.
- `((ImmutableViaClassTest) i.clone()).getUniqueNumbers().add(10000)` needs a getter that returns a live shared list. After the getter returns `Collections.unmodifiableList`, that `add` throws. `UnmodifiableGetterShallowClone` prints that rejection, then shows the leak that is still there: `clone()` aliased the `ArrayList`, and writing that list updates the original and the clone.

## 1. `SharesCallerList`

```java
this.uniqueNumbers = uniqueNumbers;
```

`final` fixes the field slot. It does not freeze the list object. The caller and the instance hold one list. `input.add(34)` is visible in `toString`.

`getUniqueNumbers()` returns that same list too. The next type leaves this getter leak as the one you can still see, after the constructor copy.

`id` is an `int` and `name` is a `String`. Callers can change their own variables. They cannot reach into this object and change those values. No copy is required for them.

## 2. `CopiesListInConstructor`

```java
this.uniqueNumbers = new ArrayList<>(uniqueNumbers);
```

`new ArrayList<>(null)` throws `NullPointerException`, so a null argument is stored as null. The copy is shallow: new list, same element references. For `Integer` that is enough, because an `Integer` has no setter.

`input.add(34)` stays on `input`. The object still prints `[1, 3]`.

```java
public List<Integer> getUniqueNumbers() {
    return uniqueNumbers;
}
```

`getUniqueNumbers().add(58)` writes the internal `ArrayList`. The object prints `[1, 3, 58]`.

Returning `new ArrayList<>(uniqueNumbers)` from the getter would also keep this object stable: the caller would edit the detached copy. The failure mode is quiet. The caller believes the object changed. Storing an immutable list and returning it makes the `add` throw. Prefer the throw.

## 3. `UnmodifiableGetterShallowClone`

The constructor still copies into an `ArrayList`. The getter returns `Collections.unmodifiableList(uniqueNumbers)` when the list is non-null.

`getUniqueNumbers().add(58)` throws `UnsupportedOperationException`. The object stays `[1, 3]`.

`unmodifiableList` does not copy. It delegates to the list you pass in. Two different fixes get confused here:

- Wrap the caller's list and skip the `ArrayList` copy. The caller still holds the backing list, so `input.add` shows through the wrapper.
- Copy, then wrap, which is what this class does. `input.add` is isolated. The internal `ArrayList` is still writable by anyone who obtains it.

This class implements `Cloneable`. `clone()` calls `super.clone()`. That copies the reference in `uniqueNumbers` and allocates no new list. The getter on the clone is the same wrapper, so `copy.getUniqueNumbers().add(10000)` throws too. The original and the clone still hold one `ArrayList`. The demo takes that list through reflection (the field is private; the alias is real) and adds `10000`. Both objects print `[1, 3, 10000]`.

`final` on the field does not matter to `clone`. The JVM writes the copied reference into the clone's field.

## 4. `CloneDisabledNotFinal`

```java
this.uniqueNumbers = List.copyOf(uniqueNumbers);
```

`List.copyOf` copies into a list that rejects `add`, `set`, and `remove`. A shallow copy of that list would share a list that rejects writes, so for `Integer` the shared reference would stay immutable. `clone()` still throws. Construction stays in the constructor, which is the path that copies. The broken alias is section 3, where the shared object is an `ArrayList`.

`clone()` throws `CloneNotSupportedException` and tells the caller to use the constructor. The class does not implement `Cloneable`. A public `clone()` that called `super.clone()` would throw the same exception for that reason. Throwing before `super.clone()` keeps the message. A null list throws `NullPointerException`. A null element throws `NullPointerException`.

The class is not `final`. `MutableChild` extends it and adds `setNickname`. This is legal:

```java
CloneDisabledNotFinal asParent = new MutableChild(1, "Manoj", numbers);
((MutableChild) asParent).setNickname("changed");
```

The list from the superclass still rejects `add`. The instance is mutable because of `nickname`. A variable whose compile-time type is the "immutable" class can point at that instance.

## 5. `FinalIntegerList`

The class is `final`, so `MutableChild` cannot extend it. Fields are `private final`. The constructor stores `List.copyOf`. Getters return the fields. `clone()` throws.

For `int`, `String`, and `List<Integer>` this is the class form to keep. Run of `main` on OpenJDK 25.0.2:

- `input.add(34)` leaves the object at `[1, 3]`
- getter `add` throws `UnsupportedOperationException`
- `clone()` throws `CloneNotSupportedException`
- `new FinalIntegerList(1, "Manoj", null)` throws `NullPointerException`
- a list containing a null element throws `NullPointerException`

`equals` and `hashCode` are not written here. Add them from the fields when this object is a value that goes into a `HashMap` or a set. `CopiedIntegerList` already has them, because a record generates both.

The record form of this section is `ImmutableViaRecordTest.CopiedIntegerList`, walked in `notes/02_immutable_via_record.md`.

## 6. `SharesMutableElements`

Same shell as `FinalIntegerList`: final class, `List.copyOf`, `clone()` throws. The element type is `Marker`, which has `setLabel`.

`input.add(new Marker("beta"))` stays out of the object. The list structure was copied.

`marker.setLabel("from-caller")` changes the object. The copy and the caller share the `Marker`.

`getMarkers().get(0).setLabel("from-getter")` changes the object again. `unmodifiable` / `List.copyOf` block structural edits. They do not wrap the elements.

`getMarkers().add(...)` throws.

## 7. `DeepCopiedMarkers`

The constructor reads `Marker.getLabel()` while it builds the object and stores `List.copyOf` of those `String`s. `String` has no setter. After `new` returns, `setLabel` on the caller's `Marker` and `add` on the caller's list leave the stored labels at the construction-time values. `getLabels().add` throws. The class is final. `clone()` throws.

That is the rule for a mutable element: copy the data you mean to keep into types that have no setters (`String`, `int`, another record of those), then store the collection with `List.copyOf`.

## Ceiling: reflection

`List.copyOf` still rejects `add` when the list is read with `field.get` after `setAccessible(true)`. The demo prints `UnsupportedOperationException`, and the object stays `[1, 3]`.

`Field.set` on the private final instance field `uniqueNumbers` is allowed on this JDK for a normal class. The demo replaces it with `List.of(7)`, and `toString` prints `[7]`. `final` stops a normal assignment in source. On OpenJDK 25.0.2 `Field.set` still replaces an instance field of a class in this module.

A record component is the exception on this JDK. `Field.set` on `CopiedIntegerList.uniqueNumbers` throws `IllegalAccessException`, and the record stays `[1, 3]`. That run is in the record chain.

Code that can call `setAccessible` on your class is already running inside the process. For a normal class, immutability is a guarantee to normal callers.

## Serializable, if you add it later

None of these types implement `Serializable`.

The lists from `List.copyOf` / `List.of` deserialize as immutable lists (the JDK writes a serialization proxy and rebuilds with `List.of`). A field that still holds an `ArrayList` deserializes as an `ArrayList`. A getter that returns that field hands out a live list on the deserialized copy, which is the getter leak again.

If you mark a class `Serializable` and the field is a collection type you control, copy again inside `readObject` the same way the constructor copies.

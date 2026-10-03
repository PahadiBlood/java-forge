# How do you make a record immutable in Java?

A record gives you a final class, private final components, a canonical constructor, and an accessor per component. It does not copy a `List`. The generated constructor stores the reference it is given, and the generated accessor returns that reference.

`ImmutableViaRecordTest` walks the same leaks as `ImmutableViaClassTest`. Each nested record closes the leak from the previous record and leaves the next leak open. `immutable.Main` calls every `demo` in both files.

Checked on the JDK in this repo: OpenJDK 25.0.2.

## What to prioritize

1. **`CopiedIntegerList` when every component is already immutable** (`int`, `String`, `Integer`). The compact constructor does `uniqueNumbers = List.copyOf(uniqueNumbers)`. The accessor is the generated one, so it returns that list. Caller `add` stays on the caller's list. Accessor `add` throws. `clone()` throws. A null list and a null element throw `NullPointerException`. `equals` and `hashCode` come with the record.

2. **`DeepCopiedMarkers.ofMarkers` when an element has a setter.** `List.copyOf` on `List<Marker>` keeps the same `Marker` objects. Read the labels into `String`s first, then let the compact constructor store `List.copyOf` of those strings.

3. **Use `FinalIntegerList` when the type has to be a normal class.** A record cannot extend a framework superclass. The class chain is `01_immutable_via_class.md`.

| Situation | What still changes the record | Record that closes it |
| --- | --- | --- |
| Constructor stores the caller's list | `input.add(...)` | `CopiesListInConstructor` |
| Accessor returns the internal `ArrayList` | `uniqueNumbers().add(...)` | `UnmodifiableAccessorShallowClone` |
| `clone()` copies the list reference | a write to that shared `ArrayList` shows on both records | `CopiedIntegerList` |
| A subclass adds a setter | cannot be written; every record is final | already closed on record 1 |
| Element type has a setter | `marker.setLabel(...)` | `DeepCopiedMarkers` |
| `Field.set` on the component | rejected on this JDK | `CopiedIntegerList` stays `[1, 3]` |

## Where the old record `main` went

The previous `Main` built one `ImmutableViaRecordTest` and wrote the list twice:

- `l.add(23)` after `new` changes the record when the constructor stores `l`. That is `SharesCallerList`. The old comment said a record does not freeze the list by itself.
- `i.ids().add(2000)` changes the record when the accessor returns the internal list. That is `CopiesListInConstructor`, after the compact constructor has copied.

The old record did both fixes in one type: an explicit canonical constructor copied with `new ArrayList<>(ids)`, and `ids()` returned `Collections.unmodifiableList`. Those are records 2 and 3. The explicit constructor and the compact constructor write the same field. The compact form reassigns the parameter; the explicit form assigns `this.uniqueNumbers`.

## 1. `SharesCallerList`

No compact constructor. `new SharesCallerList(1, "Manoj", input)` stores `input`. `uniqueNumbers()` returns that same instance (`==` is true). `input.add(34)` prints `[1, 3, 34]`.

`id` is an `int` and `name` is a `String`. Those components have no setter. The list is the component that moves.

The record is already final. The open-class leak from `CloneDisabledNotFinal` has no record equivalent: a subclass with `setNickname` does not compile.

## 2. `CopiesListInConstructor`

```java
CopiesListInConstructor {
    if (uniqueNumbers != null) {
        uniqueNumbers = new ArrayList<>(uniqueNumbers);
    }
}
```

The compact constructor runs before the field is assigned. The parameter name is the component name. Assigning `uniqueNumbers = new ArrayList<>(...)` is what gets stored. `new ArrayList<>(null)` throws, so a null argument is left null and the field is null.

`input.add(34)` leaves the record at `[1, 3]`. The accessor returns the new `ArrayList`, so `uniqueNumbers().add(58)` prints `[1, 3, 58]`.

The copy is shallow. For `Integer` the elements have no setter, so the shallow copy is enough. For `Marker` it is not. That leak is record 5.

The old file wrote this copy as an explicit canonical constructor:

```java
public ImmutableViaRecordTest(String name, List<Integer> ids) {
    this.name = name;
    this.ids = ids != null ? new ArrayList<>(ids) : null;
}
```

That assigns the field directly. The compact constructor assigns the parameter, and the compiler assigns the field from the parameter. Same stored list.

## 3. `UnmodifiableAccessorShallowClone`

The compact constructor still copies into an `ArrayList`. The accessor is declared, so the generated accessor is not emitted:

```java
@Override
public List<Integer> uniqueNumbers() {
    if (uniqueNumbers == null) {
        return null;
    }
    return Collections.unmodifiableList(uniqueNumbers);
}
```

In that method `uniqueNumbers` is the field. `uniqueNumbers()` is this method. Calling `uniqueNumbers()` from inside it recurses until the stack overflows.

`uniqueNumbers().add(58)` throws `UnsupportedOperationException`. The record stays `[1, 3]`.

`Collections.unmodifiableList` is a view of the `ArrayList` stored in the field. This record implements `Cloneable`, and `clone()` calls `super.clone()`. That copies the field reference. Accessor `add` on the clone throws too. The original and the clone hold one `ArrayList`. The demo reads that field and adds `10000`. Both records print `[1, 3, 10000]`.

Wrapping the caller's list and skipping the `ArrayList` copy would leave `input.add` visible through the view. This record copies, then wraps. `input.add` is isolated. The internal `ArrayList` is still writable by anything that obtains it.

## 4. `CopiedIntegerList`

```java
CopiedIntegerList {
    name = Objects.requireNonNull(name, "name");
    uniqueNumbers = List.copyOf(uniqueNumbers);
}
```

`List.copyOf` copies into a list that rejects `add`, `set`, and `remove`. The generated accessor returns that list. There is no `ArrayList` left to share. `clone()` throws `CloneNotSupportedException`. The record is final.

Run of `Main` on OpenJDK 25.0.2:

- `input.add(34)` leaves the record at `[1, 3]`
- accessor `add` throws `UnsupportedOperationException`
- `clone()` throws `CloneNotSupportedException`
- a null list throws `NullPointerException`
- a null element throws `NullPointerException`
- `new CopiedIntegerList(object.id(), object.name(), object.uniqueNumbers())` equals the original

This record implements `Serializable`. Deserialization of a record calls the canonical constructor, so the compact constructor runs and `List.copyOf` copies again. The restored accessor rejects `add`. The deserialized record equals the original.

That is the form to write for `int`, `String`, and `List<Integer>`.

## 5. `SharesMutableElements`

Same shell: final record, `markers = List.copyOf(markers)`, `clone()` throws. The element type is `Marker`, which has `setLabel`.

`input.add(new Marker("beta"))` stays out of the record. `marker.setLabel("from-caller")` changes it. `markers().get(0).setLabel("from-getter")` changes it again. `markers().add(...)` throws.

`List.copyOf` copies the list structure. The `Marker` objects are the same objects the caller holds.

## 6. `DeepCopiedMarkers`

The component type is `List<String>`, not `List<Marker>`. A compact constructor can replace a list. It cannot change a component from `Marker` to `String`. The factory does that read before `new`:

```java
static DeepCopiedMarkers ofMarkers(int id, String name, List<Marker> markers) {
    return new DeepCopiedMarkers(id, name, markers.stream().map(Marker::getLabel).toList());
}
```

The compact constructor then stores `List.copyOf` of those strings. After `ofMarkers` returns, `setLabel` on the caller's `Marker` and `add` on the caller's list leave the stored labels at the construction-time values. `labels().add` throws. `clone()` throws.

## Ceiling: reflection

`field.get` on `uniqueNumbers` returns the `List.copyOf` list. `add` on that list throws `UnsupportedOperationException`. The record stays `[1, 3]`.

`Field.set` of `List.of(7)` onto that component throws `IllegalAccessException` on OpenJDK 25.0.2 (`Can not set final java.util.List field ...CopiedIntegerList.uniqueNumbers`). The record stays `[1, 3]`.

The same `Field.set` against `FinalIntegerList` in the class chain succeeds and the object prints `[7]`. The record component is the case this JDK refuses. The class field is the case it allows.

# How do you make a record immutable in Java?

A record gives you a lot for free:

- the class is `final`
- each field is private and `final`
- you get a constructor
- you get a getter for each field (a record calls it an accessor)

A record does not copy a `List`. The constructor stores the list you pass in. The getter returns that same list.

`ImmutableViaRecordTest` walks the same problems as the class note. Read `01_immutable_via_class.md` first. Each record here fixes the problem from the record before it, and leaves the next problem open.

Run `immutable.Main`. It runs the class demos, then these record demos.

These checks were run on OpenJDK 25.0.2.

## Which one should you keep?

1. Use `CopiedIntegerList` when every value is already safe. That means `int`, `String`, and `Integer`. The short constructor does `uniqueNumbers = List.copyOf(uniqueNumbers)`. The getter returns that list. The caller's `add` stays on the caller's list. `add` on the getter throws. `clone()` throws. A null list and a null item throw `NullPointerException`. A record already writes `equals` and `hashCode`.
2. Use `DeepCopiedMarkers.ofMarkers` when an item has a setter. `List.copyOf` on `List<Marker>` keeps the same `Marker` objects. Read the labels into `String` values first. Then store `List.copyOf` of those strings.
3. Use the class `FinalIntegerList` when you need a normal class. A record cannot extend another class. That walk is in `01_immutable_via_class.md`.

| What is still wrong | The record that fixes it |
| --- | --- |
| The constructor stores the caller's list. `input.add(...)` changes the record | `CopiesListInConstructor` |
| The getter returns the real `ArrayList`. `uniqueNumbers().add(...)` changes the record | `UnmodifiableAccessorShallowClone` |
| `clone()` shares that `ArrayList`. A write shows up on both records | `CopiedIntegerList` |
| A child class could add a setter | Already fixed. Every record is `final` |
| An item has a setter. `marker.setLabel(...)` changes the record | `DeepCopiedMarkers` |
| `Field.set` tries to replace the field | On this JDK the record stays `[1, 3]` |

## 1. SharesCallerList

**Issue:** People think a record freezes every field. It does not freeze a list.

This record has no extra constructor code. `new SharesCallerList(1, "Manoj", input)` stores `input`. `uniqueNumbers()` returns that same list. `input.add(34)` prints `[1, 3, 34]`.

`id` is an `int`. `name` is a `String`. Those have no setter. The list is the part that moves.

The record is already `final`. The open-class problem from the class note cannot be written here. A child class with `setNickname` does not compile.

**Fix:** The next record copies the list before it is stored.

## 2. CopiesListInConstructor

**Issue:** Record 1 stores the caller's list. The caller's `add` changes the record.

**Fix:** A compact constructor runs before the field is saved. The parameter name is the field name. This assignment is what gets stored:

```java
CopiesListInConstructor {
    if (uniqueNumbers != null) {
        uniqueNumbers = new ArrayList<>(uniqueNumbers);
    }
}
```

`input.add(34)` leaves the record at `[1, 3]`.

`new ArrayList<>(null)` throws, so this code leaves a null argument as null. The field is then null.

The copy is a new list with the same item objects. That is enough for `Integer`. It is not enough for `Marker`. That problem is record 5.

An older version wrote the copy as a full constructor and assigned `this.ids`. The compact form assigns the parameter, and the compiler stores the field from that parameter. The saved list is the same.

**Issue still open:** The getter returns the new `ArrayList`. `uniqueNumbers().add(58)` prints `[1, 3, 58]`.

## 3. UnmodifiableAccessorShallowClone

**Issue:** Record 2 copies the list, but the getter gives the real `ArrayList` back.

**Fix:** The compact constructor still copies into an `ArrayList`. This record writes its own getter, so the generated getter is not used:

```java
@Override
public List<Integer> uniqueNumbers() {
    if (uniqueNumbers == null) {
        return null;
    }
    return Collections.unmodifiableList(uniqueNumbers);
}
```

Inside that method, the name `uniqueNumbers` is the field. The call `uniqueNumbers()` is this method. If you call `uniqueNumbers()` from inside it, the method calls itself until the stack overflows.

`uniqueNumbers().add(58)` throws `UnsupportedOperationException`. The record stays `[1, 3]`.

`Collections.unmodifiableList` is only a window over the `ArrayList` in the field. If you wrap the caller's list and skip the copy, `input.add` still shows through the window. This record copies first, then wraps. `input.add` is safe. The inner `ArrayList` is still writable by anything that gets it.

**Issue still open:** This record implements `Cloneable`. `clone()` calls `super.clone()`. That copies the field, not the list. `add` on the clone's getter throws too. The original and the clone hold one `ArrayList`. The demo reads that field and adds `10000`. Both records print `[1, 3, 10000]`.

## 4. CopiedIntegerList

**Issue:** Record 3 still shares one `ArrayList` through `clone()`.

**Fix:**

```java
CopiedIntegerList {
    name = Objects.requireNonNull(name, "name");
    uniqueNumbers = List.copyOf(uniqueNumbers);
}
```

`List.copyOf` makes a list that rejects `add`, `set`, and `remove`. The generated getter returns that list. There is no `ArrayList` left to share. `clone()` throws `CloneNotSupportedException`. The record is `final`.

On OpenJDK 25.0.2, `Main` shows:

- `input.add(34)` leaves the record at `[1, 3]`
- `add` on the getter throws `UnsupportedOperationException`
- `clone()` throws `CloneNotSupportedException`
- a null list throws `NullPointerException`
- a null item throws `NullPointerException`
- `new CopiedIntegerList(object.id(), object.name(), object.uniqueNumbers())` equals the original

This record implements `Serializable`. When a record is loaded back from bytes, Java calls the constructor again. The compact constructor runs, and `List.copyOf` copies again. The loaded record rejects `add`, and it equals the original.

This is the record to write for `int`, `String`, and `List<Integer>`.

**Issue still open:** If an item has a setter, `List.copyOf` does not protect that item.

## 5. SharesMutableElements

**Issue:** Copying the list is not the same as copying the items.

**Fix for the list shape:** The record is final. It does `markers = List.copyOf(markers)`. `clone()` throws. `input.add(new Marker("beta"))` stays out of the record. `markers().add(...)` throws.

**Issue still open:** `Marker` has `setLabel`. The copy and the caller share the same `Marker` objects.

- `marker.setLabel("from-caller")` changes the record
- `markers().get(0).setLabel("from-getter")` changes it again

## 6. DeepCopiedMarkers

**Issue:** Record 5 shares the `Marker` objects. A setter on a `Marker` changes the record.

**Fix:** The stored type is `List<String>`, not `List<Marker>`. A compact constructor can replace a list. It cannot change `Marker` into `String`. A factory method does that read before `new`:

```java
static DeepCopiedMarkers ofMarkers(int id, String name, List<Marker> markers) {
    return new DeepCopiedMarkers(id, name, markers.stream().map(Marker::getLabel).toList());
}
```

The compact constructor then stores `List.copyOf` of those strings. After `ofMarkers` returns:

- `setLabel` on the caller's `Marker` does not change the stored labels
- `add` on the caller's list does not change the stored labels
- `labels().add` throws
- `clone()` throws

## The limit: reflection

**Issue:** On a normal class, `Field.set` can replace a private final field. The class note shows that on `FinalIntegerList`. The object prints `[7]`.

**Fix on a record:** `field.get` on `uniqueNumbers` returns the `List.copyOf` list. `add` on that list throws `UnsupportedOperationException`. The record stays `[1, 3]`.

On OpenJDK 25.0.2, `Field.set` of `List.of(7)` onto that record field throws `IllegalAccessException`. The message says it cannot set the final field `CopiedIntegerList.uniqueNumbers`. The record stays `[1, 3]`.

So on this JDK, the record field is the case Java refuses. The class field is the case Java allows.

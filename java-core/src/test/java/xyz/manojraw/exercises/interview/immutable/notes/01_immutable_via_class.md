# How do you make a class immutable in Java?

Immutable means this: after the constructor finishes, normal code can read the object, but it cannot change it.

`int` and `String` already work that way. A `List` does not. The list still accepts `add`. The caller may still hold the same list.

`ImmutableViaClassTest` shows this one step at a time. Each class fixes the problem from the class before it. Each class also leaves the next problem open.

Run `immutable.Main`. It runs every class demo, then the record demos.

These checks were run on OpenJDK 25.0.2.

## Which one should you keep?

1. Prefer a record when every value is already safe to share. That means `int`, `String`, and `Integer`. The record to keep is `CopiedIntegerList`. Read `02_immutable_via_record.md`.
2. If an item inside the list has a setter, copy the values first, then store them. That class is `DeepCopiedMarkers`.
3. If you must use a normal class, keep `FinalIntegerList`. A record cannot extend some other class.
4. Store `List.copyOf(...)` in the field, and return that field. `Collections.unmodifiableList` is only a window over another list. It is a good middle step. It is a bad place to stop. Anyone who still holds the real `ArrayList` can change every window.

| What is still wrong | The class that fixes it |
| --- | --- |
| The constructor stores the caller's list. `input.add(...)` changes the object | `CopiesListInConstructor` |
| The getter returns the real `ArrayList`. `getUniqueNumbers().add(...)` changes the object | `UnmodifiableGetterShallowClone` |
| `clone()` shares that `ArrayList`. A write shows up on both objects | `CloneDisabledNotFinal` |
| The class is open. A child class can add a setter | `FinalIntegerList` |
| An item in the list has a setter. `marker.setLabel(...)` changes the object | `DeepCopiedMarkers` |
| Reflection can replace a private final field on a normal class | No class in this file fully stops that. See the last section |

## 1. SharesCallerList

**Issue:** The constructor does this:

```java
this.uniqueNumbers = uniqueNumbers;
```

`final` only stops you from pointing the field at a different list. It does not freeze the list. The caller and the object hold one list. `input.add(34)` shows up in `toString`.

The getter returns that same list too. The next class leaves that getter problem open on purpose.

`id` is an `int`. `name` is a `String`. The caller can change their own variables. They cannot change these two values inside the object. You do not need to copy them.

**Fix:** The next class copies the list in the constructor.

## 2. CopiesListInConstructor

**Issue:** Class 1 kept the caller's list.

**Fix:**

```java
this.uniqueNumbers = new ArrayList<>(uniqueNumbers);
```

`input.add(34)` now stays on the caller's list. The object still prints `[1, 3]`.

The code stores null when the argument is null. It does that because `new ArrayList<>(null)` throws `NullPointerException`. The copy is a new list, but the items inside are the same objects. That is enough for `Integer`, because `Integer` has no setter.

**Issue still open:** The getter returns the real list:

```java
public List<Integer> getUniqueNumbers() {
    return uniqueNumbers;
}
```

`getUniqueNumbers().add(58)` writes the list inside the object. The object prints `[1, 3, 58]`.

You could return `new ArrayList<>(uniqueNumbers)` from the getter. The object would stay the same, but the caller would think their `add` changed it. That failure is quiet. A list that throws on `add` is clearer. Prefer the throw.

## 3. UnmodifiableGetterShallowClone

**Issue:** Class 2 copies the list, but the getter gives the real `ArrayList` back.

**Fix:** The constructor still copies into an `ArrayList`. The getter returns `Collections.unmodifiableList(uniqueNumbers)` when the list is not null.

`getUniqueNumbers().add(58)` throws `UnsupportedOperationException`. The object stays `[1, 3]`.

`unmodifiableList` does not copy. It is only a window over the list you pass in. Two different fixes get mixed up here:

- If you wrap the caller's list and skip the copy, the caller still holds the real list. `input.add` still shows through the window.
- This class copies first, then wraps. `input.add` is safe. The `ArrayList` inside the object is still writable by anyone who gets it.

**Issue still open:** This class implements `Cloneable`. `clone()` calls `super.clone()`. That copies the field, not the list. The original and the clone hold one `ArrayList`.

`add` on the getter still throws, on both objects. The demo reads the private field and adds `10000`. Both objects print `[1, 3, 10000]`.

`final` on the field does not stop `clone`. The JVM writes the copied reference into the clone.

## 4. CloneDisabledNotFinal

**Issue:** Class 3 still shares one `ArrayList` through `clone()`.

**Fix:**

```java
this.uniqueNumbers = List.copyOf(uniqueNumbers);
```

`List.copyOf` makes a list that rejects `add`, `set`, and `remove`. A copy of that list would still reject writes. For `Integer`, sharing that list would stay safe.

`clone()` throws `CloneNotSupportedException`. The message says to use the constructor. The class does not implement `Cloneable`. A null list throws `NullPointerException`. A null item throws `NullPointerException`.

**Issue still open:** The class is not `final`. `MutableChild` extends it and adds `setNickname`. This is legal:

```java
CloneDisabledNotFinal asParent = new MutableChild(1, "Manoj", numbers);
((MutableChild) asParent).setNickname("changed");
```

The list from the parent still rejects `add`. The object can still change because of `nickname`. A variable of the "immutable" type can point at that child.

## 5. FinalIntegerList

**Issue:** Class 4 can be extended. A child can add a setter.

**Fix:** The class is `final`, so `MutableChild` cannot extend it. The fields are `private final`. The constructor stores `List.copyOf`. The getters return the fields. `clone()` throws.

For `int`, `String`, and `List<Integer>`, this is the class to keep.

On OpenJDK 25.0.2, `main` shows:

- `input.add(34)` leaves the object at `[1, 3]`
- `add` on the getter throws `UnsupportedOperationException`
- `clone()` throws `CloneNotSupportedException`
- `new FinalIntegerList(1, "Manoj", null)` throws `NullPointerException`
- a list with a null item throws `NullPointerException`

This class does not write `equals` and `hashCode`. Add them from the fields if you put the object in a `HashMap` or a `Set`. The record `CopiedIntegerList` already has both, because a record writes them for you.

The record version of this step is in `02_immutable_via_record.md`.

**Issue still open:** If an item in the list has a setter, copying the list is not enough. The next class shows that.

## 6. SharesMutableElements

**Issue:** `List.copyOf` copies the list. It keeps the same item objects.

**Fix for the list shape:** This class is final, uses `List.copyOf`, and `clone()` throws. `input.add(new Marker("beta"))` stays out of the object. `getMarkers().add(...)` throws.

**Issue still open:** `Marker` has `setLabel`.

- `marker.setLabel("from-caller")` changes the object. The copy and the caller share that `Marker`.
- `getMarkers().get(0).setLabel("from-getter")` changes the object again.

`List.copyOf` blocks `add` on the list. It does not protect the items.

## 7. DeepCopiedMarkers

**Issue:** Class 6 shares the `Marker` objects. A setter on a `Marker` changes the stored object.

**Fix:** The constructor reads each `getLabel()` while it builds the object. It stores `List.copyOf` of those `String` values. A `String` has no setter.

After `new` returns:

- `setLabel` on the caller's `Marker` does not change the stored labels
- `add` on the caller's list does not change the stored labels
- `getLabels().add` throws

The class is final. `clone()` throws.

That is the rule for a changeable item. Copy the data you want to keep into a type with no setter (`String`, `int`, or a record of those). Then store the collection with `List.copyOf`.

## The limit: reflection

**Issue:** `Field.set` can replace a private final field on a normal class.

`List.copyOf` still rejects `add` when the list is read with `field.get` after `setAccessible(true)`. The demo prints `UnsupportedOperationException`. The object stays `[1, 3]`.

On OpenJDK 25.0.2, `Field.set` on the field `uniqueNumbers` is allowed for this class. The demo replaces it with `List.of(7)`. `toString` prints `[7]`. `final` stops a normal assignment in source code. It does not stop this `Field.set` on a class in this module.

**Fix:** A record component is different on this JDK. `Field.set` on `CopiedIntegerList.uniqueNumbers` throws `IllegalAccessException`. The record stays `[1, 3]`. That run is in the record note.

Code that can call `setAccessible` is already running inside your program. For a normal class, immutability is a promise to normal callers.

## If you add Serializable later

**Issue:** None of these classes implement `Serializable` today. If you add it, the default save-and-load builds a new object and does not use your constructor.

Lists from `List.copyOf` and `List.of` come back as lists that reject `add`. The JDK rebuilds them with `List.of`. A field that still holds an `ArrayList` comes back as an `ArrayList`. A getter that returns that field gives out a live list again.

**Fix:** If the class implements `Serializable`, copy again inside `readObject`, the same way the constructor copies.

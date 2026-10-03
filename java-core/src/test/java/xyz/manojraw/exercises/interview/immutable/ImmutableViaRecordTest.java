package xyz.manojraw.exercises.interview.immutable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The same lesson as ImmutableViaClassTest, written as records.
 * Each record fixes the problem from the one before it, and leaves a new problem open.
 * Main calls every demo.
 *
 * 1. SharesCallerList keeps the caller's list.
 * 2. CopiesListInConstructor copies the list, but the accessor gives that list back.
 * 3. UnmodifiableAccessorShallowClone wraps the accessor. clone still shares the list.
 * 4. CopiedIntegerList uses List.copyOf. A record is final, so nobody can extend it.
 * 5. SharesMutableElements still shares a Marker that can change.
 * 6. DeepCopiedMarkers copies the label text into strings.
 *
 * reflectionCeiling is the same last check as the class lesson.
 */
public final class ImmutableViaRecordTest {

    /**
     * There is no custom constructor. The record stores the caller's list as it is.
     * uniqueNumbers() returns that same list.
     * If the caller later adds a number, this record changes too.
     * A record does not copy a list for you.
     */
    record SharesCallerList(int id, String name, List<Integer> uniqueNumbers) {

        static void demo() {
            header("record 1. SharesCallerList — constructor stores the caller's list");
            List<Integer> input = mutableList(1, 3);
            SharesCallerList object = new SharesCallerList(1, "Manoj", input);
            System.out.println("before: " + object);
            expect(object.uniqueNumbers() == input, "accessor returns the caller's list");

            input.add(34);
            System.out.println("after input.add(34): " + object);
            expect(List.of(1, 3, 34).equals(object.uniqueNumbers()),
                    "caller add changes the record");
        }
    }

    /**
     * The compact constructor copies the numbers into a new list.
     * Assigning uniqueNumbers here replaces the argument. That new list is what gets stored.
     * The accessor returns that list, so uniqueNumbers().add still changes this record.
     * new ArrayList of null throws, so a null input stays null.
     */
    record CopiesListInConstructor(int id, String name, List<Integer> uniqueNumbers) {

        CopiesListInConstructor {
            if (uniqueNumbers != null) {
                uniqueNumbers = new ArrayList<>(uniqueNumbers);
            }
        }

        static void demo() {
            header("record 2. CopiesListInConstructor — copy in the compact constructor, live list from the accessor");
            List<Integer> input = mutableList(1, 3);
            CopiesListInConstructor object = new CopiesListInConstructor(1, "Manoj", input);

            input.add(34);
            System.out.println("after input.add(34): " + object);
            expect(List.of(1, 3).equals(object.uniqueNumbers()),
                    "caller add stays on the caller's list");
            expect(object.uniqueNumbers() != input, "accessor returns the copied list");

            object.uniqueNumbers().add(58);
            System.out.println("after uniqueNumbers().add(58): " + object);
            expect(List.of(1, 3, 58).equals(object.uniqueNumbers()),
                    "accessor add changes the record");
        }
    }

    /**
     * The compact constructor copies the list. The accessor returns a wrapper that throws on add.
     * Inside that method, uniqueNumbers means the field. uniqueNumbers() would call the method again.
     * The wrapper still looks at the inner list. clone copies the reference, so both records share one list.
     * add on the accessor throws. Writing the shared list changes both records.
     */
    record UnmodifiableAccessorShallowClone(int id, String name, List<Integer> uniqueNumbers)
            implements Cloneable {

        UnmodifiableAccessorShallowClone {
            if (uniqueNumbers != null) {
                uniqueNumbers = new ArrayList<>(uniqueNumbers);
            }
        }

        @Override
        public List<Integer> uniqueNumbers() {
            if (uniqueNumbers == null) {
                return null;
            }
            return Collections.unmodifiableList(uniqueNumbers);
        }

        @Override
        public UnmodifiableAccessorShallowClone clone() {
            try {
                return (UnmodifiableAccessorShallowClone) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
        }

        @SuppressWarnings("unchecked")
        static void demo() throws Exception {
            header("record 3. UnmodifiableAccessorShallowClone — accessor wrapped, clone shares the ArrayList");
            List<Integer> input = mutableList(1, 3);
            UnmodifiableAccessorShallowClone original = new UnmodifiableAccessorShallowClone(1, "Manoj", input);

            input.add(34);
            System.out.println("after input.add(34): " + original);
            expect(List.of(1, 3).equals(original.uniqueNumbers()),
                    "caller add stays on the caller's list");

            expectThrows(() -> original.uniqueNumbers().add(58),
                    "accessor add rejected");
            expect(List.of(1, 3).equals(original.uniqueNumbers()),
                    "record unchanged after rejected accessor add");

            UnmodifiableAccessorShallowClone copy = original.clone();
            expectThrows(() -> copy.uniqueNumbers().add(10000),
                    "clone accessor add rejected");

            Field field = UnmodifiableAccessorShallowClone.class.getDeclaredField("uniqueNumbers");
            field.setAccessible(true);
            List<Integer> originalBacking = (List<Integer>) field.get(original);
            List<Integer> cloneBacking = (List<Integer>) field.get(copy);
            System.out.println("original and clone hold the same ArrayList: " + (originalBacking == cloneBacking));
            expect(originalBacking == cloneBacking, "shallow clone shares the list");
            System.out.println("unmodifiableList is a view of that ArrayList; writing the ArrayList updates both records");

            originalBacking.add(10000);
            System.out.println("after writing the shared ArrayList, original: " + original);
            System.out.println("after writing the shared ArrayList, clone:    " + copy);
            expect(List.of(1, 3, 10000).equals(original.uniqueNumbers()),
                    "shared list write shows on the original");
            expect(List.of(1, 3, 10000).equals(copy.uniqueNumbers()),
                    "shared list write shows on the clone");
        }
    }

    /**
     * The compact constructor stores List.copyOf. The accessor returns that list, so add throws.
     * There is no plain ArrayList left for clone to share. clone throws.
     * A record is final, so a child class with a setter cannot be written.
     * Loading this record from a saved copy runs the constructor again, so List.copyOf runs again.
     */
    record CopiedIntegerList(int id, String name, List<Integer> uniqueNumbers) implements Serializable {

        CopiedIntegerList {
            name = Objects.requireNonNull(name, "name");
            uniqueNumbers = List.copyOf(uniqueNumbers);
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            throw new CloneNotSupportedException("Clone is disabled. Build another instance with the constructor.");
        }

        static void demo() throws Exception {
            header("record 4. CopiedIntegerList — List.copyOf, record is final");
            expect(Modifier.isFinal(CopiedIntegerList.class.getModifiers()), "record is final");

            List<Integer> input = mutableList(1, 3);
            CopiedIntegerList object = new CopiedIntegerList(1, "Manoj", input);
            input.add(34);
            System.out.println("after input.add(34): " + object);
            expect(List.of(1, 3).equals(object.uniqueNumbers()),
                    "caller add stays on the caller's list");

            expectThrows(() -> object.uniqueNumbers().add(58),
                    "accessor add rejected");

            try {
                object.clone();
                throw new IllegalStateException("clone should fail");
            } catch (CloneNotSupportedException e) {
                System.out.println("clone rejected -> " + e.getMessage());
            }

            expectThrows(() -> new CopiedIntegerList(1, "Manoj", null),
                    "null list rejected");

            List<Integer> withNullElement = new ArrayList<>();
            withNullElement.add(1);
            withNullElement.add(null);
            expectThrows(() -> new CopiedIntegerList(1, "Manoj", withNullElement),
                    "null element rejected");

            CopiedIntegerList again = new CopiedIntegerList(object.id(), object.name(), object.uniqueNumbers());
            expect(again.equals(object), "second instance built from the accessor matches");

            byte[] bytes;
            try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                 ObjectOutputStream objectOut = new ObjectOutputStream(out)) {
                objectOut.writeObject(object);
                bytes = out.toByteArray();
            }
            try (ObjectInputStream objectIn = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
                CopiedIntegerList restored = (CopiedIntegerList) objectIn.readObject();
                System.out.println("after deserialize: " + restored);
                expect(restored.equals(object), "deserialized record matches");
                expectThrows(() -> restored.uniqueNumbers().add(58),
                        "deserialized accessor add rejected");
            }
        }
    }

    /**
     * A small object with a setter. List.copyOf on a list of Marker copies the references, not the text.
     */
    static final class Marker {

        private String label;

        Marker(String label) {
            this.label = label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * Same shape as CopiedIntegerList: a final record, List.copyOf, and clone throws.
     * Marker.setLabel still changes this record. The accessor rejects add.
     */
    record SharesMutableElements(int id, String name, List<Marker> markers) {

        SharesMutableElements {
            name = Objects.requireNonNull(name, "name");
            markers = List.copyOf(markers);
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            throw new CloneNotSupportedException("Clone is disabled. Build another instance with the constructor.");
        }

        static void demo() {
            header("record 5. SharesMutableElements — immutable list of mutable elements");
            Marker marker = new Marker("alpha");
            List<Marker> input = new ArrayList<>();
            input.add(marker);

            SharesMutableElements object = new SharesMutableElements(1, "Manoj", input);
            input.add(new Marker("beta"));
            System.out.println("after input.add(beta): " + object);
            expect(object.markers().size() == 1, "list copy keeps later adds out");

            marker.setLabel("from-caller");
            System.out.println("after caller marker.setLabel: " + object);
            expect("from-caller".equals(object.markers().get(0).getLabel()),
                    "caller setLabel changes the record");

            object.markers().get(0).setLabel("from-getter");
            System.out.println("after accessor marker.setLabel: " + object);
            expect("from-getter".equals(object.markers().get(0).getLabel()),
                    "accessor setLabel changes the record");

            expectThrows(() -> object.markers().add(new Marker("gamma")),
                    "list add rejected");
        }
    }

    /**
     * This record stores strings, not Marker objects.
     * ofMarkers reads each label before new. The constructor then stores a list that rejects add.
     * A later setLabel changes only the caller's Marker.
     */
    record DeepCopiedMarkers(int id, String name, List<String> labels) {

        DeepCopiedMarkers {
            name = Objects.requireNonNull(name, "name");
            labels = List.copyOf(labels);
        }

        static DeepCopiedMarkers ofMarkers(int id, String name, List<Marker> markers) {
            return new DeepCopiedMarkers(id, name, markers.stream().map(Marker::getLabel).toList());
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            throw new CloneNotSupportedException("Clone is disabled. Build another instance with the constructor.");
        }

        static void demo() {
            header("record 6. DeepCopiedMarkers — element values copied into Strings");
            expect(Modifier.isFinal(DeepCopiedMarkers.class.getModifiers()), "record is final");

            Marker marker = new Marker("alpha");
            List<Marker> input = new ArrayList<>();
            input.add(marker);

            DeepCopiedMarkers object = DeepCopiedMarkers.ofMarkers(1, "Manoj", input);
            input.add(new Marker("beta"));
            marker.setLabel("from-caller");
            System.out.println("after caller mutates the marker and the list: " + object);
            expect(List.of("alpha").equals(object.labels()),
                    "stored labels stay at construction-time values");

            expectThrows(() -> object.labels().add("gamma"),
                    "label list add rejected");

            try {
                object.clone();
                throw new IllegalStateException("clone should fail");
            } catch (CloneNotSupportedException e) {
                System.out.println("clone rejected -> " + e.getMessage());
            }
        }
    }

    /**
     * List.copyOf still rejects add when reflection reads the list back.
     * On this JDK, Field.set is refused for a record field, so the list stays as it was.
     */
    static void reflectionCeiling() throws Exception {
        header("record ceiling — reflection on CopiedIntegerList");
        CopiedIntegerList object = new CopiedIntegerList(1, "Manoj", mutableList(1, 3));
        Field field = CopiedIntegerList.class.getDeclaredField("uniqueNumbers");
        field.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Object> backing = (List<Object>) field.get(object);
        expectThrows(() -> backing.add(999),
                "reflected List.copyOf rejected add");
        System.out.println("after reflected add attempt: " + object);

        try {
            field.set(object, List.of(7));
            throw new IllegalStateException("Field.set replaced a record component");
        } catch (IllegalAccessException | InaccessibleObjectException e) {
            System.out.println("Field.set rejected -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        expect(List.of(1, 3).equals(object.uniqueNumbers()),
                "record component stayed in place");
    }

    private static List<Integer> mutableList(Integer... values) {
        return new ArrayList<>(List.of(values));
    }

    private static void header(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    private static void expect(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static void expectThrows(Runnable action, String message) {
        try {
            action.run();
        } catch (RuntimeException e) {
            System.out.println(message + " -> " + e.getClass().getSimpleName());
            return;
        }
        throw new IllegalStateException(message + " did not throw");
    }
}

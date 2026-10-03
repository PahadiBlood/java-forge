package xyz.manojraw.exercises.interview.immutable;

import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Each class below fixes the problem from the class before it, and leaves a new problem open.
 * Main calls every demo.
 *
 * 1. SharesCallerList keeps the caller's list.
 * 2. CopiesListInConstructor copies the list, but the getter gives that list back.
 * 3. UnmodifiableGetterShallowClone wraps the getter. clone still shares the list.
 * 4. CloneDisabledNotFinal stores a list that rejects add, and clone throws. The class can be extended.
 * 5. FinalIntegerList is final. This is enough when the list holds Integer.
 * 6. SharesMutableElements uses the same protection, but Marker can still be changed.
 * 7. DeepCopiedMarkers copies the label text, so later changes to Marker do not show up.
 *
 * reflectionCeiling shows that reflection can still replace a private final field.
 * The record version of this lesson is ImmutableViaRecordTest.
 */
public final class ImmutableViaClassTest {

    /**
     * The constructor keeps the list the caller passed in.
     * If the caller later adds a number, this object changes too.
     * id is an int and name is a String, so those two fields cannot be changed from outside.
     */
    static final class SharesCallerList {

        private final int id;
        private final String name;
        private final List<Integer> uniqueNumbers;

        SharesCallerList(int id, String name, List<Integer> uniqueNumbers) {
            this.id = id;
            this.name = name;
            this.uniqueNumbers = uniqueNumbers;
        }

        public List<Integer> getUniqueNumbers() {
            return uniqueNumbers;
        }

        @Override
        public String toString() {
            return "SharesCallerList{id=" + id + ", name='" + name + "', uniqueNumbers=" + uniqueNumbers + "}";
        }

        static void demo() {
            header("class 1. SharesCallerList — constructor stores the caller's list");
            List<Integer> input = mutableList(1, 3);
            SharesCallerList object = new SharesCallerList(1, "Manoj", input);
            System.out.println("before: " + object);

            input.add(34);
            System.out.println("after input.add(34): " + object);
            expect(List.of(1, 3, 34).equals(object.getUniqueNumbers()),
                    "caller add changes the object");
        }
    }

    /**
     * The constructor copies the numbers into a new list, so a later add on the caller's list stays there.
     * new ArrayList of null throws, so a null input is stored as null.
     * The getter returns the inner list, so getUniqueNumbers().add still changes this object.
     */
    static final class CopiesListInConstructor {

        private final int id;
        private final String name;
        private final List<Integer> uniqueNumbers;

        CopiesListInConstructor(int id, String name, List<Integer> uniqueNumbers) {
            this.id = id;
            this.name = name;
            this.uniqueNumbers = uniqueNumbers != null ? new ArrayList<>(uniqueNumbers) : null;

        }

        public List<Integer> getUniqueNumbers() {
            return uniqueNumbers;
        }

        @Override
        public String toString() {
            return "CopiesListInConstructor{id=" + id + ", name='" + name + "', uniqueNumbers=" + uniqueNumbers + "}";
        }

        static void demo() {
            header("class 2. CopiesListInConstructor — copy in the constructor, live list from the getter");
            List<Integer> input = mutableList(1, 3);
            CopiesListInConstructor object = new CopiesListInConstructor(1, "Manoj", input);

            input.add(34);
            System.out.println("after input.add(34): " + object);
            expect(List.of(1, 3).equals(object.getUniqueNumbers()),
                    "caller add stays on the caller's list");

            object.getUniqueNumbers().add(58);
            System.out.println("after getUniqueNumbers().add(58): " + object);
            expect(List.of(1, 3, 58).equals(object.getUniqueNumbers()),
                    "getter add changes the object");
        }
    }

    /**
     * The constructor copies the list. The getter returns a wrapper that throws if you call add.
     * That wrapper still looks at the inner list.
     * clone copies the reference, so the original and the clone share one list.
     * Writing that shared list changes both objects. add on the getter still throws.
     */
    static final class UnmodifiableGetterShallowClone implements Cloneable {

        private final int id;
        private final String name;
        private final List<Integer> uniqueNumbers;

        UnmodifiableGetterShallowClone(int id, String name, List<Integer> uniqueNumbers) {
            this.id = id;
            this.name = name;
            this.uniqueNumbers = uniqueNumbers != null ? new ArrayList<>(uniqueNumbers) : null;
        }

        public List<Integer> getUniqueNumbers() {
            if (uniqueNumbers == null) {
                return null;
            }
            return Collections.unmodifiableList(uniqueNumbers);
        }

        @Override
        public UnmodifiableGetterShallowClone clone() {
            try {
                return (UnmodifiableGetterShallowClone) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        public String toString() {
            return "UnmodifiableGetterShallowClone{id=" + id
                    + ", name='" + name + "', uniqueNumbers=" + uniqueNumbers + "}";
        }

        @SuppressWarnings("unchecked")
        static void demo() throws Exception {
            header("class 3. UnmodifiableGetterShallowClone — getter wrapped, clone shares the ArrayList");
            List<Integer> input = mutableList(1, 3);
            UnmodifiableGetterShallowClone original = new UnmodifiableGetterShallowClone(1, "Manoj", input);

            input.add(34);
            System.out.println("after input.add(34): " + original);
            expect(List.of(1, 3).equals(original.getUniqueNumbers()),
                    "caller add stays on the caller's list");

            expectThrows(() -> original.getUniqueNumbers().add(58),
                    "getter add rejected");
            expect(List.of(1, 3).equals(original.getUniqueNumbers()),
                    "object unchanged after rejected getter add");

            UnmodifiableGetterShallowClone copy = original.clone();
            expectThrows(() -> copy.getUniqueNumbers().add(10000),
                    "clone getter add rejected");

            Field field = UnmodifiableGetterShallowClone.class.getDeclaredField("uniqueNumbers");
            field.setAccessible(true);
            List<Integer> originalBacking = (List<Integer>) field.get(original);
            List<Integer> cloneBacking = (List<Integer>) field.get(copy);
            System.out.println("original and clone hold the same ArrayList: " + (originalBacking == cloneBacking));
            expect(originalBacking == cloneBacking, "shallow clone shares the list");
            System.out.println("unmodifiableList is a view of that ArrayList; writing the ArrayList updates both objects");

            originalBacking.add(10000);
            System.out.println("after writing the shared ArrayList, original: " + original);
            System.out.println("after writing the shared ArrayList, clone:    " + copy);
            expect(List.of(1, 3, 10000).equals(original.getUniqueNumbers()),
                    "shared list write shows on the original");
            expect(List.of(1, 3, 10000).equals(copy.getUniqueNumbers()),
                    "shared list write shows on the clone");
        }
    }

    /**
     * The stored list comes from List.copyOf, so add throws. There is no plain ArrayList left to share.
     * clone throws.
     * The class is not final. MutableChild adds a setter, so an object of this type can still change its nickname.
     */
    static class CloneDisabledNotFinal {

        private final int id;
        private final String name;
        private final List<Integer> uniqueNumbers;

        CloneDisabledNotFinal(int id, String name, List<Integer> uniqueNumbers) {
            this.id = id;
            this.name = Objects.requireNonNull(name, "name");
            this.uniqueNumbers = List.copyOf(uniqueNumbers);
        }

        public List<Integer> getUniqueNumbers() {
            return uniqueNumbers;
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            throw new CloneNotSupportedException("Clone is disabled. Build another instance with the constructor.");
        }

        @Override
        public String toString() {
            return "CloneDisabledNotFinal{id=" + id + ", name='" + name + "', uniqueNumbers=" + uniqueNumbers + "}";
        }

        static void demo() {
            header("class 4. CloneDisabledNotFinal — immutable list, clone rejected, class is open");
            List<Integer> input = mutableList(1, 3);
            CloneDisabledNotFinal object = new CloneDisabledNotFinal(1, "Manoj", input);

            input.add(34);
            System.out.println("after input.add(34): " + object);
            expect(List.of(1, 3).equals(object.getUniqueNumbers()),
                    "caller add stays on the caller's list");

            expectThrows(() -> object.getUniqueNumbers().add(58),
                    "getter add rejected");

            try {
                object.clone();
                throw new IllegalStateException("clone should fail");
            } catch (CloneNotSupportedException e) {
                System.out.println("clone rejected -> " + e.getMessage());
            }

            expect(!Modifier.isFinal(CloneDisabledNotFinal.class.getModifiers()),
                    "class is open for a subclass");

            CloneDisabledNotFinal asParent = new MutableChild(1, "Manoj", mutableList(1, 3));
            System.out.println("held as CloneDisabledNotFinal: " + asParent);
            ((MutableChild) asParent).setNickname("changed");
            System.out.println("nickname after setter: " + ((MutableChild) asParent).getNickname());
            expect("changed".equals(((MutableChild) asParent).getNickname()),
                    "subclass setter changes the object");
            expectThrows(() -> asParent.getUniqueNumbers().add(58),
                    "subclass list still rejects add");
        }
    }

    /**
     * A child of CloneDisabledNotFinal. The list still rejects add.
     * nickname has a setter, so the object can still change.
     */
    static final class MutableChild extends CloneDisabledNotFinal {

        private String nickname;

        MutableChild(int id, String name, List<Integer> uniqueNumbers) {
            super(id, name, uniqueNumbers);
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getNickname() {
            return nickname;
        }
    }

    /**
     * Final class, private final fields, List.copyOf in the constructor, and clone throws.
     * Integer cannot be changed, so this closes the problems above for this kind of data.
     * The record version is ImmutableViaRecordTest.CopiedIntegerList.
     */
    static final class FinalIntegerList {

        private final int id;
        private final String name;
        private final List<Integer> uniqueNumbers;

        FinalIntegerList(int id, String name, List<Integer> uniqueNumbers) {
            this.id = id;
            this.name = Objects.requireNonNull(name, "name");
            this.uniqueNumbers = List.copyOf(uniqueNumbers);
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public List<Integer> getUniqueNumbers() {
            return uniqueNumbers;
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            throw new CloneNotSupportedException("Clone is disabled. Build another instance with the constructor.");
        }

        @Override
        public String toString() {
            return "FinalIntegerList{id=" + id + ", name='" + name + "', uniqueNumbers=" + uniqueNumbers + "}";
        }

        static void demo() {
            header("class 5. FinalIntegerList — final class, immutable Integer list");
            expect(Modifier.isFinal(FinalIntegerList.class.getModifiers()), "class is final");

            List<Integer> input = mutableList(1, 3);
            FinalIntegerList object = new FinalIntegerList(1, "Manoj", input);
            input.add(34);
            System.out.println("after input.add(34): " + object);
            expect(List.of(1, 3).equals(object.getUniqueNumbers()),
                    "caller add stays on the caller's list");

            expectThrows(() -> object.getUniqueNumbers().add(58),
                    "getter add rejected");

            try {
                object.clone();
                throw new IllegalStateException("clone should fail");
            } catch (CloneNotSupportedException e) {
                System.out.println("clone rejected -> " + e.getMessage());
            }

            expectThrows(() -> new FinalIntegerList(1, "Manoj", null),
                    "null list rejected");

            List<Integer> withNullElement = new ArrayList<>();
            withNullElement.add(1);
            withNullElement.add(null);
            expectThrows(() -> new FinalIntegerList(1, "Manoj", withNullElement),
                    "null element rejected");
        }
    }

    /**
     * A small object with a setter. Copying a list of Marker copies the references, not the text.
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
     * Same protection as FinalIntegerList: final class, List.copyOf, and clone throws.
     * Marker can still change. List.copyOf copies the list but keeps the same Marker objects,
     * so setLabel on the caller's marker changes this object.
     * getMarkers().add still throws.
     */
    static final class SharesMutableElements {

        private final int id;
        private final String name;
        private final List<Marker> markers;

        SharesMutableElements(int id, String name, List<Marker> markers) {
            this.id = id;
            this.name = Objects.requireNonNull(name, "name");
            this.markers = List.copyOf(markers);
        }

        public List<Marker> getMarkers() {
            return markers;
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            throw new CloneNotSupportedException("Clone is disabled. Build another instance with the constructor.");
        }

        @Override
        public String toString() {
            return "SharesMutableElements{id=" + id + ", name='" + name + "', markers=" + markers + "}";
        }

        static void demo() {
            header("class 6. SharesMutableElements — immutable list of mutable elements");
            Marker marker = new Marker("alpha");
            List<Marker> input = new ArrayList<>();
            input.add(marker);

            SharesMutableElements object = new SharesMutableElements(1, "Manoj", input);
            input.add(new Marker("beta"));
            System.out.println("after input.add(beta): " + object);
            expect(object.getMarkers().size() == 1, "list copy keeps later adds out");

            marker.setLabel("from-caller");
            System.out.println("after caller marker.setLabel: " + object);
            expect("from-caller".equals(object.getMarkers().get(0).getLabel()),
                    "caller setLabel changes the object");

            object.getMarkers().get(0).setLabel("from-getter");
            System.out.println("after getter marker.setLabel: " + object);
            expect("from-getter".equals(object.getMarkers().get(0).getLabel()),
                    "getter setLabel changes the object");

            expectThrows(() -> object.getMarkers().add(new Marker("gamma")),
                    "list add rejected");
        }
    }

    /**
     * Copies each Marker label into a String when the object is built, then stores those strings
     * with List.copyOf. A later setLabel changes only the caller's Marker.
     * The stored list rejects add. The class is final and clone throws.
     */
    static final class DeepCopiedMarkers {

        private final int id;
        private final String name;
        private final List<String> labels;

        DeepCopiedMarkers(int id, String name, List<Marker> markers) {
            this.id = id;
            this.name = Objects.requireNonNull(name, "name");
            this.labels = List.copyOf(markers.stream().map(Marker::getLabel).toList());
        }

        public List<String> getLabels() {
            return labels;
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            throw new CloneNotSupportedException("Clone is disabled. Build another instance with the constructor.");
        }

        @Override
        public String toString() {
            return "DeepCopiedMarkers{id=" + id + ", name='" + name + "', labels=" + labels + "}";
        }

        static void demo() {
            header("class 7. DeepCopiedMarkers — element values copied into Strings");
            expect(Modifier.isFinal(DeepCopiedMarkers.class.getModifiers()), "class is final");

            Marker marker = new Marker("alpha");
            List<Marker> input = new ArrayList<>();
            input.add(marker);

            DeepCopiedMarkers object = new DeepCopiedMarkers(1, "Manoj", input);
            input.add(new Marker("beta"));
            marker.setLabel("from-caller");
            System.out.println("after caller mutates the marker and the list: " + object);
            expect(List.of("alpha").equals(object.getLabels()),
                    "stored labels stay at construction-time values");

            expectThrows(() -> object.getLabels().add("gamma"),
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
     * Field.set on a private final field is a separate question. This method prints what this JDK does.
     */
    static void reflectionCeiling() throws Exception {
        header("class ceiling — reflection on FinalIntegerList");
        FinalIntegerList object = new FinalIntegerList(1, "Manoj", mutableList(1, 3));
        Field field = FinalIntegerList.class.getDeclaredField("uniqueNumbers");
        field.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Object> backing = (List<Object>) field.get(object);
        expectThrows(() -> backing.add(999),
                "reflected List.copyOf rejected add");
        System.out.println("after reflected add attempt: " + object);

        try {
            field.set(object, List.of(7));
        } catch (ReflectiveOperationException | InaccessibleObjectException e) {
            System.out.println("Field.set failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return;
        }
        System.out.println("Field.set replaced uniqueNumbers -> " + object);
        expect(List.of(7).equals(object.getUniqueNumbers()),
                "Field.set replaced the private final list");
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

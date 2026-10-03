package xyz.manojraw.exercises.interview.singleton;

// The object is created when the class loads, even if nobody calls getInstance.
// The JVM does that setup on one thread, so this form needs no synchronized and no volatile.
// final stops normal code from replacing the field later.
// A private constructor does not stop reflection. See the notes.
public class SingletonExampleEager {

    private static final SingletonExampleEager instance = new SingletonExampleEager();

    private SingletonExampleEager() {
    }

    public static SingletonExampleEager getInstance() {
        return instance;
    }
}

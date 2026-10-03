package xyz.manojraw.exercises.interview.singleton;

// The object is created the first time getInstance runs.
// The JVM sets up the Holder class on one thread, so later reads need no lock and no volatile.
// Use this when you need a normal class. The enum is simpler when a class is not required.
// Reflection, save-and-load, and clone can still make a second object. See the notes.
public class SingletonExampleLazyViaHolder {

    private SingletonExampleLazyViaHolder() {
    }

    private static class Holder {
        private static final SingletonExampleLazyViaHolder INSTANCE = new SingletonExampleLazyViaHolder();
    }

    public static SingletonExampleLazyViaHolder getInstance() {
        return Holder.INSTANCE;
    }
}

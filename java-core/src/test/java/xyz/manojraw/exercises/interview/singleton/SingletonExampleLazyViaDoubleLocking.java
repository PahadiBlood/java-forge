package xyz.manojraw.exercises.interview.singleton;

// Safe for normal calls. Reflection can still create a second object.
public class SingletonExampleLazyViaDoubleLocking {

    // volatile is required.
    // new does three things: reserve memory, run the constructor, then store the address in s.
    // Without volatile, another thread can see s before the constructor has finished,
    // and then use an object that is only half built.
    private static volatile SingletonExampleLazyViaDoubleLocking s;

    private SingletonExampleLazyViaDoubleLocking() {
    }

    public static SingletonExampleLazyViaDoubleLocking getInstance() {
        if (s == null) {
            // Threads that arrive together take turns. Only one of them creates the object.
            synchronized (SingletonExampleLazyViaDoubleLocking.class) {
                if (s == null) {
                    s = new SingletonExampleLazyViaDoubleLocking();
                }
            }
        }
        return s;
    }
}

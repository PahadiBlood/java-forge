package xyz.manojraw.exercises.interview.singleton;

// Every call takes the class lock, even after the object already exists.
// That is safe. It is a poor choice when getInstance is called often,
// because threads still line up just to return the same object.
public class SingletonExampleLazyViaSynchronizedMethod {

    private static SingletonExampleLazyViaSynchronizedMethod s;

    private SingletonExampleLazyViaSynchronizedMethod() {
    }

    public static synchronized SingletonExampleLazyViaSynchronizedMethod getInstance() {
        if (s == null) {
            s = new SingletonExampleLazyViaSynchronizedMethod();
        }
        return s;
    }
}

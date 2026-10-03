package xyz.manojraw.exercises.interview.singleton;

// Use this when you need one shared object and this type does not have to extend another class.
// INSTANCE is created once, the first time the enum is used.
// The JVM blocks a second object from reflection, from clone, and from save-and-load.
public enum SingletonExampleLazyViaEnum {
    INSTANCE
}

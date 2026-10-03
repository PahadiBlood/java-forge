package xyz.manojraw.exercises.interview.singleton;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Starts several threads for each singleton and checks that they all saw one object.
 * identityHashCode follows the real object, so a custom hashCode cannot hide a second one.
 *
 * The same id means the threads agreed. This test does not check reflection,
 * save-and-load, or a second class loader.
 */
public class SingletonThreadTest {

    public static void main(String[] args) throws InterruptedException {
        check("eager", SingletonExampleEager::getInstance);
        check("synchronized-method", SingletonExampleLazyViaSynchronizedMethod::getInstance);
        check("double-checked", SingletonExampleLazyViaDoubleLocking::getInstance);
        check("holder", SingletonExampleLazyViaHolder::getInstance);
        check("enum", () -> SingletonExampleLazyViaEnum.INSTANCE);
    }

    private static void check(String label, Supplier<Object> get) throws InterruptedException {
        var ids = ConcurrentHashMap.<Integer>newKeySet();
        var threads = new Thread[8];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                Object instance = get.get();
                int id = System.identityHashCode(instance);
                ids.add(id);
                System.out.println(Thread.currentThread().getName() + " " + label + " " + id);
            }, label + "-" + i);
            threads[i].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        System.out.println(label + " distinct instances: " + ids.size()
                + (ids.size() == 1 ? " (one instance)" : " (more than one instance)"));
        System.out.println();
    }
}

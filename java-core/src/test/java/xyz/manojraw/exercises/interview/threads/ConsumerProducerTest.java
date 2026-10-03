package xyz.manojraw.exercises.interview.threads;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

/**
 * Coffee shop with one shared order queue.
 *
 * placeOrder adds a brew task and wakes the consumer.
 * processOrder waits for tasks, then starts one thread per order.
 * They never call each other. The queue is the only thing they share.
 */
public class ConsumerProducerTest {

    // This queue is also the lock. Both sides must synchronize on this same object.
    private static final Queue<Runnable> orders = new ArrayDeque<>();

    /** Runs on the thread that calls it. In this demo that thread is main. */
    public String placeOrder(String name) {
        // Build the task before taking the lock. The lock should only guard the queue.
        Runnable brewTask = () -> {
            try {
                // Set the name inside the task, so it is ready when the work starts.
                Thread.currentThread().setName(name);
                Thread.sleep(3000); // pretend the coffee takes 3 seconds
                System.out.println("Order is completed for customer " + name);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        synchronized (orders) {          // same object the consumer waits on
            orders.add(brewTask);
            orders.notify();             // wake the consumer. The lock is still held until this block ends.
        }
        System.out.println("Coffee ordered successfully for customer " + name);
        return UUID.randomUUID().toString();
    }

    /** Meant to run forever on its own thread. */
    public void processOrder() throws InterruptedException {
        while (true) {                       // outside the lock, so the producer can get in between passes
            synchronized (orders) {
                while (orders.isEmpty()) {   // check again after every wake-up
                    orders.wait();           // lets go of the lock while this thread waits
                }
                while (!orders.isEmpty()) {  // take every order that is already waiting
                    Thread.startVirtualThread(orders.poll()); // this already starts the thread
                }
            }
        }
    }

    public static void main(String[] args) {
        ConsumerProducerTest shop = new ConsumerProducerTest();

        // This thread keeps the program alive. The brew threads are virtual threads,
        // and those do not keep the program alive on their own.
        Thread consumer = new Thread(() -> {
            try {
                shop.processOrder();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "order-consumer");
        consumer.start();

        System.out.println("Order place started");
        List.of("Manoj", "Yoga", "Chamal", "GoGo").forEach(shop::placeOrder);
        // main ends here; the program keeps running because the consumer never returns.
    }
}
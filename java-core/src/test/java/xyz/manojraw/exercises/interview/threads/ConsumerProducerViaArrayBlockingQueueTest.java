package xyz.manojraw.exercises.interview.threads;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * One producer and one consumer share a small queue.
 *
 * The producer adds numbers with put. If 5 numbers are already waiting, put waits.
 * The consumer takes a number with take. If the queue is empty, take waits.
 * Then the consumer sleeps for 1 second.
 *
 * They never call each other. The queue is the only thing they share.
 *
 * ConsumerProducerTest does the same idea with synchronized, wait, and notify.
 * ArrayBlockingQueue already does that waiting for you.
 * The notes file 02_array_blocking_queue.md explains why this is the better everyday choice.
 */
public class ConsumerProducerViaArrayBlockingQueueTest {

    // The queue can hold only 5 numbers. put waits when it is full.
    // It does not grow, so a fast producer cannot fill memory.
    // By default a later thread can jump ahead of one that is already waiting.
    // new ArrayBlockingQueue<>(5, true) makes them wait in arrival order.
    private static final ArrayBlockingQueue<Integer> nums = new ArrayBlockingQueue<>(5);

    // One thread runs produce. The other runs consume.
    // These threads keep the program alive after main returns.
    // This demo never shuts the pool down. Stop the program yourself.
    private static final ExecutorService service = Executors.newFixedThreadPool(2);

    /**
     * Keeps running until the thread is interrupted.
     * The inner loop adds 0 to 999. The outer loop starts that list again.
     * After 5 numbers are waiting, put waits until take removes one.
     */
    public void produce() {
        Thread.currentThread().setName("producer");
        while (true) {
            for (int i = 0; i < 1000; i++) {
                try {
                    nums.put(i);
                    System.out.println("Added num " + i);
                } catch (InterruptedException e) {
                    // put clears the interrupt mark when it throws. Put the mark back and stop.
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    /**
     * Keeps running until the thread is interrupted.
     * take waits while the queue is empty.
     * The sleep is after take, so the producer can add a number during that second.
     */
    public void consume() {
        Thread.currentThread().setName("consumer");
        while (true) {
            try {
                int i = nums.take();
                System.out.println("Take num " + i);
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public static void main(String[] args) {
        ConsumerProducerViaArrayBlockingQueueTest cp = new ConsumerProducerViaArrayBlockingQueueTest();

        // Order does not matter. take waits for the first put.
        // put waits once the queue is full.
        service.execute(cp::produce);
        service.execute(cp::consume);
        // main ends here. The two pool threads keep the program running.
    }
}

package Java_Multithreading;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Shared bounded queue between producers and consumers.
 *
 * Concepts shown:
 * - Intrinsic locking with synchronized methods
 * - wait()/notifyAll() for producer-consumer coordination
 * - ConcurrentHashMap for shared stats
 * - ReadWriteLock for protecting read/write access to stats
 */
public class TaskQueue {

    // ----- Producer / Consumer queue using synchronized + wait/notify -----
    private final Queue<String> tasks = new LinkedList<>();
    private final int maxSize = 5;

    /**
     * Adds a task to the queue. If the queue is full, the calling
     * producer thread waits until space becomes available.
     */
    public synchronized void putTask(String task){
        while (tasks.size() == maxSize) {
            System.out.println(Thread.currentThread().getName() + " is waiting: queue is FULL");
            try {
                wait();
            } catch (InterruptedException e) {
                // If interrupted while waiting, exit early
                Thread.currentThread().interrupt();
                return;
            }
        }
        tasks.add(task);
        System.out.println(Thread.currentThread().getName() + " produced " + task);
        notifyAll();
    }

    /**
     * Removes and returns a task from the queue. If the queue is empty,
     * the calling consumer thread waits until a task is produced.
     */
    public synchronized String takeTask() {
        try {
            while (tasks.isEmpty()) {
                System.out.println(Thread.currentThread().getName() + " is waiting: queue is EMPTY");
                wait();
            }
            String task = tasks.poll();
            notifyAll();
            return task;
        } catch (InterruptedException e) {
            // Propagate interruption to allow graceful shutdown
            Thread.currentThread().interrupt();
            return null;
        }
    }

    // ----- Stats using concurrent collections + ReadWriteLock -----

    // Stores how many tasks each consumer thread processed in total
    private final Map<String, Integer> processedPerThread = new ConcurrentHashMap<>();

    private final ReadWriteLock statsLock = new ReentrantReadWriteLock();
    private final Lock readLock = statsLock.readLock();
    private final Lock writeLock = statsLock.writeLock();

    /**
     * Called by consumers after they successfully take a task.
     * Uses write lock because we are mutating the stats map.
     */
    public void recordProcessed(String threadName) {
        writeLock.lock();
        try {
            processedPerThread.merge(threadName, 1, Integer::sum);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Prints per-thread processing stats. Uses the read lock so
     * multiple readers could read concurrently if needed.
     */
    public void printStats() {
        readLock.lock();
        try {
            System.out.println("\nProcessed tasks per consumer thread:");
            if (processedPerThread.isEmpty()) {
                System.out.println("(no tasks processed)");
                return;
            }
            for (Map.Entry<String, Integer> entry : processedPerThread.entrySet()) {
                System.out.println(entry.getKey() + " -> " + entry.getValue());
            }
        } finally {
            readLock.unlock();
        }
    }
}

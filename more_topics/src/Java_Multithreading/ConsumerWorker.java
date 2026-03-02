package Java_Multithreading;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConsumerWorker implements Runnable{

    private final TaskQueue taskQueue;
    // Volatile stop flag so main thread can signal this worker to stop
    private volatile boolean isRunning = true;

    // Atomic and lock-based counters to demonstrate atomic classes and ReentrantLock
    private static final AtomicInteger atomicProcessedCount = new AtomicInteger(0);
    private static final Lock manualLock = new ReentrantLock();
    private static int manualProcessedCount = 0;

    public ConsumerWorker(TaskQueue taskQueue){
        this.taskQueue = taskQueue;
    }

    /**
     * Called from main thread to request this worker to stop.
     * The volatile flag ensures visibility across threads.
     */
    public void stopRunning() {
        isRunning = false;
    }

    @Override
    public void run(){
        while (isRunning) {
            String task = taskQueue.takeTask();
            if (task == null) {
                // Interrupted or shutting down
                break;
            }
            System.out.println(Thread.currentThread().getName() + " is taking task " + task);

            // Update shared counters:
            // - atomicProcessedCount: lock-free atomic increment
            // - manualProcessedCount: increment protected by ReentrantLock
            atomicProcessedCount.incrementAndGet();
            manualLock.lock();
            try {
                manualProcessedCount++;
            } finally {
                manualLock.unlock();
            }

            // Record stats in TaskQueue (ConcurrentHashMap + ReadWriteLock)
            taskQueue.recordProcessed(Thread.currentThread().getName());

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println(Thread.currentThread().getName() + " is stopping. " +
                "Atomic processed so far = " + atomicProcessedCount.get() +
                ", manual processed so far = " + manualProcessedCount);
    }
}

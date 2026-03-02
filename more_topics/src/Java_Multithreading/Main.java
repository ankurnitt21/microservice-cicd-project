package Java_Multithreading;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Main {

    /**
     * Entry point for all multithreading demos.
     *
     * 1) First runs producer-consumer using plain Thread / Runnable / synchronized / wait-notify.
     * 2) Then shows the same idea using ExecutorService + Callable + Future.
     * 3) Finally shows a small CompletableFuture async pipeline.
     */
    public static void main(String[] args){
        System.out.println("=== Producer-Consumer Demo with Threads ===");
        // Shared queue using synchronized + wait/notify + ReadWriteLock stats
        TaskQueue taskQueue = new TaskQueue();

        // Classic producer threads using Thread subclass
        ProducerThread producerThread1 = new ProducerThread(taskQueue);
        ProducerThread producerThread2 = new ProducerThread(taskQueue);

        producerThread1.setName("ProducerThread1");
        producerThread2.setName("ProducerThread2");

        // Consumer workers implement Runnable and use a volatile flag to stop
        ConsumerWorker worker1 = new ConsumerWorker(taskQueue);
        ConsumerWorker worker2 = new ConsumerWorker(taskQueue);
        Thread consumerWorker1 = new Thread(worker1, "ConsumerWorker1");
        Thread consumerWorker2 = new Thread(worker2, "ConsumerWorker2");

        producerThread1.start();
        producerThread2.start();
        consumerWorker1.start();
        consumerWorker2.start();

        try{
            // Wait for both producers to finish creating tasks
            producerThread1.join();
            producerThread2.join();

            // Let consumers process remaining tasks still in the queue
            Thread.sleep(2000);

            // Signal consumers to stop using the volatile flag
            worker1.stopRunning();
            worker2.stopRunning();

            // Interrupt consumers in case they are waiting or sleeping
            consumerWorker1.interrupt();
            consumerWorker2.interrupt();

            consumerWorker1.join();
            consumerWorker2.join();
        } catch(InterruptedException e){
            e.printStackTrace();
        }

        System.out.println("All threads are finished");
        // Print per-thread stats built using ConcurrentHashMap + ReadWriteLock
        taskQueue.printStats();

        // Same producer-consumer idea implemented using ExecutorService + Callable + Future
        runExecutorServiceDemo();

        // Small CompletableFuture example (async pipeline)
        runCompletableFutureDemo();

        // Optional: deadlock demonstration (WILL hang if you uncomment)
        // runDeadlockDemo();
    }

    /**
     * Demonstrates producer-consumer using a thread pool:
     * - Producers and consumers are modeled as Callable<Integer>
     * - We submit them to an ExecutorService and get results via Future
     * - Still reuses the same TaskQueue implementation.
     */
    private static void runExecutorServiceDemo() {
        System.out.println("\n=== Producer-Consumer with ExecutorService & Future ===");
        // Separate queue for this demo (so it doesn't interfere with the first one)
        TaskQueue queue = new TaskQueue();
        ExecutorService executor = Executors.newFixedThreadPool(4);

        // Producer Callable: produces 5 tasks into the queue and returns how many it produced
        Callable<Integer> producerTask = () -> {
            String name = Thread.currentThread().getName();
            for (int i = 0; i < 5; i++) {
                queue.putTask(name + "-Task-" + i);
            }
            return 5;
        };

        // Consumer Callable: consumes 5 tasks from the queue and returns how many it consumed
        Callable<Integer> consumerTask = () -> {
            int processed = 0;
            String name = Thread.currentThread().getName();
            while (processed < 5) {
                String task = queue.takeTask();
                System.out.println(name + " processed (pool) " + task);
                processed++;
            }
            return processed;
        };

        try {
            // Submit two producers and two consumers to the pool
            Future<Integer> p1 = executor.submit(producerTask);
            Future<Integer> p2 = executor.submit(producerTask);
            Future<Integer> c1 = executor.submit(consumerTask);
            Future<Integer> c2 = executor.submit(consumerTask);

            int produced = p1.get() + p2.get();
            int consumed = c1.get() + c2.get();

            System.out.println("ExecutorService produced tasks = " + produced +
                    ", consumed tasks = " + consumed);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Demonstrates a simple CompletableFuture pipeline:
     * - supplyAsync runs on a worker thread
     * - thenApply transforms the value in stages
     * - thenAccept consumes the final result.
     */
    private static void runCompletableFutureDemo() {
        System.out.println("\n=== CompletableFuture Demo ===");

        CompletableFuture<Integer> future =
                CompletableFuture.supplyAsync(() -> {
                    System.out.println("supplyAsync on " + Thread.currentThread().getName());
                    return 10;
                })
                .thenApply(x -> {
                    System.out.println("thenApply on " + Thread.currentThread().getName());
                    return x * 2;
                })
                .thenApply(x -> x + 5); // final result 25

        future.thenAccept(result ->
                System.out.println("CompletableFuture final result = " + result));

        try {
            future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

}

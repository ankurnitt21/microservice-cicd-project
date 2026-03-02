package MultiThreading;
import java.util.concurrent.*;

public class ThreadPool {

    public static void main(String[] args) throws Exception {

        // 1. Create Thread Pool
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // ---------------- Runnable using execute() ----------------
        Runnable executeTask = () -> {
            System.out.println(Thread.currentThread().getName() +
                    " running execute() Runnable task");
        };

        executor.execute(executeTask);   // No Future returned


        // ---------------- Runnable using submit() ----------------
        Runnable runnableTask = () -> {
            System.out.println(Thread.currentThread().getName() +
                    " running submit() Runnable task");
        };

        Future<?> runnableFuture = executor.submit(runnableTask);


        // ---------------- Callable using submit() ----------------
        Callable<Integer> callableTask = () -> {
            System.out.println(Thread.currentThread().getName() +
                    " running Callable task");

            Thread.sleep(2000);  // simulate work
            return 200;
        };

        Future<Integer> callableFuture = executor.submit(callableTask);


        // ---------------- Check Status ----------------
        System.out.println("Runnable done? " + runnableFuture.isDone());
        System.out.println("Callable done? " + callableFuture.isDone());


        // ---------------- Get Result ----------------
        runnableFuture.get();   // wait (no return value)

        Integer result = callableFuture.get();   // wait & get result
        System.out.println("Callable Result = " + result);


        // Shutdown Thread Pool
        executor.shutdown();
    }
}

// Execute Runnable task using execute() method of ExecutorService. No Future is returned, so we cannot check the status or get the result of the task.
// Execute Runnable task using submit() method of ExecutorService. A Future is returned, so we can check the status of the task, but since it's a Runnable, there is no result to get.
// Execute Callable task using submit() method of ExecutorService. A Future is returned, so we can check the status of the task and get the result of the task. Callable tasks can return a result.
// execute() is non blocking and does not return a Future, while submit() is blocking and returns a Future due to future.get() method call. If we don't call future.get(), submit() will also be non blocking and will not wait for the task to complete.
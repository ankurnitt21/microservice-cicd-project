package JVM_Memory;

import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates key JVM memory concepts:
 * - Stack vs Heap (recursive stack growth)
 * - Heap usage and OutOfMemoryError pattern
 * - String pool vs normal heap strings
 *
 * NOTE: This class is intentionally not executed by tooling.
 * Run manually if you want to observe behaviour.
 */
public class JvmMemoryDemo {

    // Used to demonstrate heap growth and a "leak-like" pattern
    private static final List<byte[]> heapFiller = new ArrayList<>();

    // Used to observe at which recursion depth the stack overflows
    private static int currentDepth = 0;

    public static void main(String[] args) {
        System.out.println("=== JVM Memory Demo ===");

        // 1. String pool vs heap strings
        System.out.println("\n--- String Pool Demo ---");
        stringPoolDemo();

        // 2. Stack vs Heap (comment out if you don't want a StackOverflowError)
        System.out.println("\n--- Stack vs Heap (StackOverflowError) Demo ---");
        try {
            growStack(1);
        } catch (StackOverflowError e) {
            System.out.println("Caught StackOverflowError at depth: " + currentDepth);
        }

        // 3. Heap usage and OutOfMemoryError pattern
        System.out.println("\n--- Heap Usage & OutOfMemoryError Demo ---");
        // WARNING: Running this with a small heap (e.g. -Xms64m -Xmx64m) will likely
        // produce an OutOfMemoryError: Java heap space.
        // Example:
        //   javac JVM_Memory/JvmMemoryDemo.java
        //   java -Xms64m -Xmx64m JVM_Memory.JvmMemoryDemo
        try {
            fillHeap();
        } catch (OutOfMemoryError e) {
            System.out.println("Caught OutOfMemoryError after allocating ~"
                    + heapFiller.size() + " MB");
        }

        // 4. Clear references so objects become eligible for GC
        System.out.println("\n--- Clearing Heap References (Eligible for GC) ---");
        heapFiller.clear();
        System.out.println("Cleared references in heapFiller; objects are now eligible for GC.");

        System.out.println("\n=== JVM Memory Demo Finished ===");
    }

    /**
     * Demonstrates the difference between:
     * - String literals in the string pool
     * - Strings created with new (heap objects)
     */
    private static void stringPoolDemo() {
        String a = "hello";
        String b = "hello";           // same literal, from string pool
        String c = new String("hello"); // new object on heap

        System.out.println("a == b        : " + (a == b));          // true, same reference from pool
        System.out.println("a == c        : " + (a == c));          // false, different object
        System.out.println("a.equals(c)   : " + a.equals(c));       // true, same value

        // Show explicit interning
        String d = c.intern(); // moves/gets reference from pool
        System.out.println("a == d (after intern): " + (a == d));   // true
    }

    /**
     * Recursively grows the call stack to demonstrate StackOverflowError.
     * Each recursive call has its own stack frame (parameters, local variables).
     */
    private static void growStack(int depth) {
        currentDepth = depth;

        // Local variable allocated on the stack; the int[] reference is on the stack,
        // while the actual array object is on the heap.
        int[] data = new int[1_000];

        if (depth % 100 == 0) {
            System.out.println("Current recursion depth: " + depth);
        }

        // Recursive call grows the stack
        growStack(depth + 1);

        // 'data' goes out of scope when this frame is popped from the stack
    }

    /**
     * Fills the heap with 1 MB blocks referenced from a static list.
     * This prevents garbage collection of those blocks and leads
     * to an OutOfMemoryError on sufficiently small heaps.
     */
    private static void fillHeap() {
        int mbAllocated = 0;
        while (true) {
            byte[] block = new byte[1024 * 1024]; // 1 MB
            heapFiller.add(block);
            mbAllocated++;

            if (mbAllocated % 10 == 0) {
                System.out.println("Allocated approximately " + mbAllocated + " MB on the heap");
            }
        }
    }
}


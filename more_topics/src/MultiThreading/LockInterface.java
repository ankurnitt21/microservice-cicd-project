package MultiThreading;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


class SbiBankAccount {
    private int balance=1000;


    private final Lock lock = new ReentrantLock(); //non-FIFO + can hold multiple locks by same thread
    private final ReentrantLock fairLock = new ReentrantLock(true); //FIFO + can hold multiple locks by same thread
    private final ReentrantLock unfairLock = new ReentrantLock(false); //non-FIFO + can hold multiple locks by same thread
    // 1st lock and 3rd lock both are same

    public void withdraw(int value){
        lock.lock();
        try {
            balance = balance - value;
            System.out.println("Balance is " + balance);
        } finally {lock.unlock();}
    }

    public void withdraw1(int value){
        if(lock.tryLock()) { // if lock is not available, it will return false immediately without waiting
            try {
                balance = balance - value;
                System.out.println("Balance is " + balance);
            } finally {
                lock.unlock();
            }
        } else {
            System.out.println("Lock not acquired");
        }
    }

    public void withdraw2(int value) throws InterruptedException {
        if(lock.tryLock(2, TimeUnit.SECONDS)) { // if lock is not available, it will wait for 2 seconds and if lock is still not available, it will return false
            try {
                balance = balance - value;
                System.out.println("Balance is " + balance);
            } finally {
                lock.unlock();
            }
        } else {
            System.out.println("Lock not acquired");
        }
    }

    public void withdraw3(int value){
        try {
            lock.lockInterruptibly(); // if lock is not available, it will wait until lock is available or thread is interrupted. If thread is interrupted while waiting for the lock, it will throw InterruptedException
            try {
                balance = balance - value;
                System.out.println("Balance is " + balance);
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            System.out.println("Thread was interrupted while waiting for the lock");
        }

    }

}

class AccountBalance {
    private int balance1=1000;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private  final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    //Many threads can read the balance at the same time, but only one thread can write to the balance at a time
    public void getBalance() {
        readLock.lock();
        try {
            System.out.println("Read Balance is " + balance1);
        } finally {readLock.unlock();}
    }

    // Only one thread can write to the balance at a time, and no thread can read the balance while it is being written
    public void withdraw(int value){
        writeLock.lock();
        try {
            balance1 = balance1 - value;
            System.out.println("After Withdraw Balance is " + balance1);
        } finally {writeLock.unlock();}
    }
}

public class LockInterface {
    public static void main(String args[]){
        SbiBankAccount account=new SbiBankAccount();

        Runnable task1 = () -> {
            account.withdraw(500);
        };
        Runnable task2 = () -> {
            account.withdraw(500);
        };

        Thread thread1 = new Thread(task1);
        Thread thread2 = new Thread(task2);
        thread1.start();
        thread2.start();

            AccountBalance accountBalance = new AccountBalance();
            Runnable readTask = () -> {
                accountBalance.getBalance();
            };
            Runnable writeTask = () -> {
                accountBalance.withdraw(500);
            };
            Thread readThread1 = new Thread(readTask);
            Thread readThread2 = new Thread(readTask);
            Thread writeThread = new Thread(writeTask);
            readThread1.start();
            readThread2.start();
            writeThread.start();
    }
}
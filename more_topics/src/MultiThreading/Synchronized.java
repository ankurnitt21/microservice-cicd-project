package MultiThreading;
class BankAccount{
    private int balance = 1000;
    public synchronized void withdraw(int amount) {
        if(balance >= amount) {
            balance -= amount;
            System.out.println("Withdrawing from bank" + amount);
        } else {
            System.out.println("Insufficient funds bank " + balance);
        }
    }
}

class Atm {
    static int totalCash = 10000;

    public static synchronized void withdraw(int amount) {
        if (totalCash >= amount) {
            totalCash -= amount;
            System.out.println("Withdrawing from atm" + amount);
        } else {
            System.out.println("ATM out of cash " + totalCash);
        }
    }
}

class sharedBuffer {
private int value;
private boolean hasValue = false;

public synchronized void produce(int value) throws InterruptedException {
    while(hasValue){wait();} // release lock and wait until consumer consumes the value
    this.value = value;
    System.out.println("Value is " + value);
    hasValue = true;
    notify(); // wake up the consumer thread
}
    public synchronized void consume() throws InterruptedException {
    while(!hasValue){wait();} // release lock and wait until producer produces a value
    System.out.println("Consumed: " + value);
    hasValue = false;
    notify(); // wake up the producer thread
    }
}

public class Synchronized {
    public static void main(String[] args) {

      //Object level lock
      BankAccount b1 = new BankAccount();
      Thread t1 = new Thread(() -> b1.withdraw(800));
      Thread t2 = new Thread(() -> b1.withdraw(800));
      t1.start();
      t2.start();

      //Class level lock
      Atm atm1 = new Atm();
      Atm atm2 = new Atm();
      Thread t3 = new Thread(() -> atm1.withdraw(6000));
      Thread t4 = new Thread(() -> atm2.withdraw(6000));
      t3.start();
      t4.start();

      // wait and notify
      sharedBuffer buffer = new sharedBuffer();
      Thread producer = new Thread(()-> {
          for(int i=0;i<5;i++){
              try {
                  buffer.produce(i);
              } catch (InterruptedException e) {
                  throw new RuntimeException(e);
              }
          }});

        Thread consumer = new Thread(()-> {for(int i=0;i<5;i++){
            try {
                buffer.consume();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }});

        producer.start();
        consumer.start();
    }
}
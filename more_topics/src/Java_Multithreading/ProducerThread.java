package Java_Multithreading;

class ProducerThread extends Thread {

    private final TaskQueue taskQueue;

    public ProducerThread(TaskQueue taskQueue){
        this.taskQueue = taskQueue;
    }

    /**
     * Simple producer loop that creates 5 tasks and puts
     * them into the shared TaskQueue, sleeping between tasks
     * to simulate work.
     */
    @Override
    public void run() {
        for(int i = 0; i < 5; i++){
            taskQueue.putTask("Task " + i);
            try{
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}

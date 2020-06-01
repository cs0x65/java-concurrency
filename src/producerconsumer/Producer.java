package producerconsumer;

import java.util.Queue;
import java.util.logging.Logger;

/**
 * Producer in the classic producer consumer paradigm.
 * The other characteristics exhibited by this class are
 * - it invokes notifyAll() post producing each new message for the consumers blocking on the queue to get notified and
 * possibly scheduled for execution.
 * - when provided non-zero value for sleepFor, it sleeps for the given time interval after producing each message;
 * this kind of helps control throttling with which the messages are being produced.
 * - when yield is set to true with setYield(true), then it tries to yield the processor control for consumer
 * and possibly other threads to be scheduled for execution; but this is all together dependent on the thread scheduler
 * of the underlying JVM - it may or may not yield the processor control.
 * - if the queue is capacity bound and there's no more space available for new messages, then it logs this fact
 * and silently terminates: this may happen if consumer couldn't keep up with the rate at which the messages are
 * being produced resulting in the messages lingering longer and occupying all of the specified capacity.
 * Typically, the {@link producerconsumer.Consumer} on the other end of the queue will halt on time-out once it
 * has consumed all the messages left in the queue and no further message being produced as Producer has already halted.
 * - if all the messages are successfully produced (for practical purpose and to keep it simple - the setup allows to
 * put some pre-defined higher limit on the number of messages being generated), then it sends the closing message with
 * value "-1" to indicate to it's consumers that it won't produce any new message and is about to halt or already halted
 * (by the time consumer consumes the closing message).
 */
public class Producer implements Runnable{
    private  Queue<String> queue;
    // Defaults to 100
    private int numMessages;
    // Defaults to 0 milliseconds
    private long sleepFor;
    private boolean yield = false;
    Logger logger = Logger.getLogger(getClass().getName());

    public Producer(Queue<String> queue, int numMessages, long sleepFor) {
        this.queue = queue;
        this.numMessages = numMessages;
        this.sleepFor = sleepFor;
    }

    public Producer(Queue<String> queue, long sleepFor) {
        this(queue, 100, sleepFor);
    }

    public Producer(Queue<String> queue) {
        this(queue, 100, 0);
    }

    public boolean isYield() {
        return yield;
    }

    public void setYield(boolean yield) {
        this.yield = yield;
    }

    @Override
    public void run() {
        logger.info("Producer starting...");
        int i;
        for (i = 0; i < numMessages; i++) {
            String message = "Message"+i;
            synchronized (queue){
                if (queue.offer(message)){
                    logger.info("Produced: "+message);
                    // notify the consumers
                    queue.notifyAll();
                }else {
                    logger.warning("Capacity exhausted: unable to enqueue message, exiting...!");
                    break;
                }
            }
            if (sleepFor > 0){
                try {
                    Thread.sleep(sleepFor);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (yield){
                Thread.yield();
            }
        }
        synchronized (queue){
            if (i == numMessages){
                logger.info("Producer halting...");
                queue.offer("-1");
            }
            queue.notifyAll();
        }
    }
}

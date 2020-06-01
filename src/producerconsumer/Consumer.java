package producerconsumer;

import java.util.Queue;
import java.util.logging.Logger;

/**
 * Consumer in the producer consumer paradigm.
 * The other characteristics exhibited by this class are:
 * - TIME_TO_WAIT_BEFORE_HALTING interval: which defines the period to wait for new message on the queue from
 * the producer; once this window has passed by and no new message has been seen then the queue halts owing to the
 * time-out.
 * - consumes the closing message with value "-1" from the producer and halts the session.
 */
public class Consumer implements Runnable{
    private  Queue<String> queue;
    private Logger logger = Logger.getLogger(getClass().getName());
    // The time in milliseconds the consumer will wait for a message to appear
    // on the queue; once this interval is elapsed and if no message ends up appearing
    // on the queue, the consumer will simply shut down.
    public static final long TIME_TO_WAIT_BEFORE_HALTING = 90 * 1000;


    public Consumer(Queue<String> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        String message = "";
        logger.info("Consumer starting...");
        while (message != "-1"){
            int timeInWaiting = 0;
            synchronized (queue){
                while (queue.peek() == null){
                    try {
                        logger.warning("Consumer waiting: queue is empty!");
                        long startTime = System.currentTimeMillis();
                        // wait() is a blocking method.
                        queue.wait(30 * 1000);
                        long elapsedTime = System.currentTimeMillis() - startTime;
                        timeInWaiting += elapsedTime;
                        logger.info("timeInWaiting = "+timeInWaiting);
                        if (timeInWaiting >= TIME_TO_WAIT_BEFORE_HALTING){
                            logger.warning("Consumer halting: no message appeared on the queue for last "+
                                    (timeInWaiting / 1000) + " seconds!");
                            return;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                message = queue.poll();
            }
            logger.info("Consumed: "+message);
        }
        logger.info("Consumer halting: producer finished generating all the messages...");
    }
}

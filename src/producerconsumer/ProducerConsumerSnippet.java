package producerconsumer;

import main.Snippet;

import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

/**
 * The setup is kept simple for now with requirement that some maximum limit be set on the Producer to
 * contain the number of messages that will be produced.
 *
 * TODO: support for scenario where knowing max number of messages being produced/consumed is not known before-hand -
 * e.g. some realtime data streams, processing feeds from social media etc. Actually, nothing changes from the
 * perspective of core functionality of the Producer - it will need to be provided with or augmented with the
 * stream/API processing, parsing, cleansing etc. to build a valid message before it's published to the queue.
 *
 * Execution instructions:
 * Run this producer-consumer snippet with below command:
 * java Main producerconsumer.ProducerConsumerSnippet <mode>
 *     where mode =
 *                1: normal/default
 *                2: producer sleeps for few milliseconds after producing each message
 *                3: producer yields after producing each message (possibly to encourage the interleaved
 *                scheduling of the threads)
 *                4: consumer halts on time-out as it doesn't receive any message on the queue for
 *                interval >= Consumer.TIME_TO_WAIT_BEFORE_HALTING; to make this happen, make sure to
 *                set Producer.sleepFor value is >  Consumer.TIME_TO_WAIT_BEFORE_HALTING.
 *
 * For e.g:   java Main producerconsumer.ProducerConsumerSnippet 1
 * TODO: make Producer & Consumer parameterized <T> rather than them assuming a type (like String for now)
 * of the queue; so that any Queue<T> can be used with it.
 *
 */
public class ProducerConsumerSnippet implements Snippet {
    Logger logger = Logger.getLogger(getClass().getName());

    @Override
    public void runSnippet(String[] args) {
        // normal mode
        int mode = 1;
        if (args != null && args.length > 0){
            mode = Integer.parseInt(args[0]);
        }
        switch (mode){
            case 1:{
                logger.info("Running normal mode");
                runNormalSnippet();
            }
            break;

            case 2:{
                logger.info("Running in 'producer sleeps for some milliseconds after producing each message' mode");
                runProducerSleepsAfterEachMessageSnippet();
            }
            break;

            case 3:{
                logger.info("Running in 'producer yields after producing each message' mode");
                runProducerYieldsAfterEachMessageSnippet();
            }
            break;

            case 4:{
                logger.info("Running in 'consumer halts on time-out' mode");
                runConsumerHaltsOnTimeoutSnippet();
            }
            break;

            default:
                logger.info("Running normal mode");
                runNormalSnippet();
        }
    }

    private void runNormalSnippet(){
        Queue<String> queue = new LinkedList<>();
        new Thread(new Consumer(queue)).start();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        new Thread(new Producer(queue)).start();
    }

    /**
     * By setting value for sleepFor, the producer will wait for given milliseconds after producing each message.
     */
    private void runProducerSleepsAfterEachMessageSnippet(){
        Queue<String> queue = new LinkedList<>();
        new Thread(new Consumer(queue)).start();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Let producer sleep for 100 ms post producing each message
        new Thread(new Producer(queue, 100)).start();
    }

    /**
     * By setting setYield(true), the producer will try to yield it's control of the processor
     * after producing each message. Yield is just a hint to the thread scheduler - it may or may not act on it.
     * This may give chance for producer & consumer threads taking turns(BUT no guarantee) instead
     * of producer finishing producing all messages and then consumer consuming all the produced ones.
     */
    private void runProducerYieldsAfterEachMessageSnippet(){
        Queue<String> queue = new LinkedList<>();
        new Thread(new Consumer(queue)).start();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Producer producer = new Producer(queue);
        producer.setYield(true);
        new Thread(producer).start();
    }

    /**
     * Set the producer sleepFor interval such that it's greater than consumer's TIME_TO_WAIT_BEFORE_HALTING.
     * In such a scenario, the consumer will fail to get any message on the queue from producer and will timeout.
     * This kind of consumer halting gives consumer thread a chance to cleanly wrap up it's execution
     * when any of the following happens -
     * - the producer/thread has been blocked for too long
     * - the producer/thread has encountered a condition such that it has been irrevocably freezed/blocked
     * or entered in an loop and isn't producing any new message.
     */
    private void runConsumerHaltsOnTimeoutSnippet(){
        Queue<String> queue = new LinkedList<>();
        Thread consumerThread =  new Thread(new Consumer(queue));
        consumerThread.setName("Consumer-Thread-cs0x65");
        consumerThread.start();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Let producer sleep for 10 ms more than Consumer.TIME_TO_WAIT_BEFORE_HALTING, so that
        // we can verify that consumer times out and halts.
        Thread producerThread = new Thread(new Producer(queue, Consumer.TIME_TO_WAIT_BEFORE_HALTING + 1000));
        producerThread.setName("Producer-Thread-cs0x65");
        producerThread.start();
    }
}

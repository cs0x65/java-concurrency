package sequencegeneration;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class EvenNumberGenerator implements Runnable{
    // acts as a monitor
    private Object lock;
    private boolean isAtomicInteger;
    private  boolean isSnippetClass;
    private boolean shallHalt;
    Logger logger = Logger.getLogger(getClass().getName());

    public EvenNumberGenerator(Object lock) {
        isAtomicInteger = lock instanceof AtomicInteger;
        isSnippetClass = lock instanceof Class &&
                ((Class) lock).getSimpleName().equals("OddEvenTurnByTurnGenerationSnippet");
        if (!(isAtomicInteger || isSnippetClass)){
            throw new IllegalArgumentException("The lock object either needs to be instance of AtomicInteger or " +
                    "OddEvenTurnByTurnGenerationSnippet class. Received lock = "+ lock);
        }
        this.lock = lock;
    }

    public void setShallHalt(boolean shallHalt) {
        this.shallHalt = shallHalt;
    }

    @Override
    public void run() {
        logger.entering(getClass().getName(), "run");
        if (isAtomicInteger){
            runWithAtomicInteger();
        }else {
            runWithSnippetClass();
        }
        logger.exiting(getClass().getName(), "run");
    }

    private void runWithAtomicInteger(){
        logger.entering(getClass().getName(), "runWithAtomicInteger");
        AtomicInteger aiLock = (AtomicInteger)lock;
        while (!shallHalt){
            synchronized (lock){
                while (aiLock.get() % 2 != 1){
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                logger.info("Next even number: "+aiLock.addAndGet(1));
                lock.notify();
            }
            try {
                // just to slow down the log/console entries for human monitoring :)
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void runWithSnippetClass(){
        logger.entering(getClass().getName(), "runWithSnippetClass");
        while (!shallHalt){
            synchronized (lock){
                while (OddEvenTurnByTurnGenerationSnippet.getCurrentNumber() % 2 != 1){
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                OddEvenTurnByTurnGenerationSnippet.setCurrentNumber(
                        OddEvenTurnByTurnGenerationSnippet.getCurrentNumber() + 1);
                logger.info("Next even number: "+OddEvenTurnByTurnGenerationSnippet.getCurrentNumber());
                lock.notify();
            }
            try {
                // just to slow down the log/console entries for human monitoring :)
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

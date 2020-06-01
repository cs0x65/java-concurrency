package sequencegeneration;

import main.Snippet;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Caveat 1
 * ========
 * It might be tempting to use something like:
 * int currentNumber = 0;
 * as a starting point and let OddNumberGenerator and EvenNumberGenerator use it as the lock/monitor, where
 * their usage pattern is like this:
 * synchronized (currentNumber){
 *  while (currentNumber % 2 != 0 (for generating odd number) or currentNumber % 2 != 1 (for generating even number)){
 *      currentNumber.wait();
 *  }
 *  currentNumber++;
 *  currentNumber.notify()
 *
 *  There's a problem with this problem:
 *  The normal int/Integer are immutable objects, when it executes
 *  currentNumber++;
 *
 *  it's effectively same as:
 *  currentNumber = currentNumber + 1;
 *
 *  and it effectively currentNumber on which the lock had been held while entering this synchronized block
 *  has all together changed to a different instance of currentNumber and hence statement like:
 *  currentNumber.notify()
 *  will throw IllegalMonitorStateException.
 *
 * Caveat 2
 * ========
 * The above observation may lead us to think that we can separate out the lock/monitor and currentNumber i.e
 * have a dedicated lock/monitor passed to OddNumberGenerator and EvenNumberGenerator instances, and
 * their usage pattern is like:
 * synchronized (lock){
 *  while (currentNumber % 2 != 0 (for generating odd number) or currentNumber % 2 != 1 (for generating even number)){
 *      lock.wait();
 *  }
 *  currentNumber++;
 *  lock.notify();
 *
 *  But each of the Even & Odd generator gets the copy of currentNumber passed to it.
 *  So when statement:
 *  currentNumber++;
 *
 *  as int/Integer is immutable, it gets executed as,
 *  currentNumber = currentNumber + 1;
 *
 *  essentially resulting into the one generator updating it's local copy of currentNumber- rather pointing to the
 *  incremented copy of currentNumber.
 *  But other generator doesn't get to see these changes as it's holding on to it's stale copy of local currentNumber
 *  and there's new way it can see the updated copy from other generator.
 *  Hence, the programs just blocks after printing the 1st odd number; EvenNumberGenerator waits forever on the
 *  condition:
 *  while (currentNumber % 2 != 1){
 *      lock.wait();
 *  }
 *
 *  On the other hand, because EvenNumberGenerator is not gonna ever get a chance to increment the number and make
 *  it even, OddNumberGenerator also waits on the condition forever:
 *  while (currentNumber % 2 != 0){
 *  *      lock.wait();
 *  *  }
 *
 * Essentially, both generators wait forever and the program enters a dead-lock.
 *
 * Solution 1
 * ==========
 * Use AtomicInteger which can be used as a monitor as well as the counter to keep track of the currentNumber
 * and the changes/increments to which are visible to both the generators.
 *
 * Solution 2
 * ==========
 * Keep the currentNumber as a static field on OddEvenTurnByTurnGenerationSnippet and pass
 * OddEvenTurnByTurnGenerationSnippet.class as a lock to be used by both the generators.
 *
 * It won't require an AtomicInteger, and instead the increments made to the currentNumber by statement(s):
 * OddEvenTurnByTurnGenerationSnippet.currentNumber++;
 * OR
 * currentNumber = OddEvenTurnByTurnGenerationSnippet.getCurrentNumber();
 * OddEvenTurnByTurnGenerationSnippet.setCurrentNumber(++currentNumber);
 *
 * is visible to both the generators.
 *
 * One option is to keep both the generator classes in the OddEvenTurnByTurnGenerationSnippet and make those
 * non-public.
 */
public class OddEvenTurnByTurnGenerationSnippet implements Snippet {
    private static int currentNumber = 0;
    private EvenNumberGenerator evenNumberGenerator;
    private OddNumberGenerator oddNumberGenerator;
    Logger logger = Logger.getLogger(getClass().getName());

    public static int getCurrentNumber() {
        return currentNumber;
    }

    public static void setCurrentNumber(int currentNumber) {
        OddEvenTurnByTurnGenerationSnippet.currentNumber = currentNumber;
    }

    @Override
    public void runSnippet(String[] args) {
        System.out.println("Press any key to terminate the program!");

        int mode = 1;
        if (args != null && args.length > 0){
            mode = Integer.parseInt(args[0]);
        }
        switch (mode) {
            case 1:
                runWithAtomicIntegerLockSnippet();
                break;

            case 2:
                runWithClassLockSnippet();
                break;

            default:
                runWithAtomicIntegerLockAndDaemonModeSnippet();
                break;
        }

        try {
            while (true){
                // halt on the 1st keyboard i/p
                System.in.read();
                break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (mode == 1 || mode == 2){
            System.out.println("Halting: cleaning up generator threads...");
            oddNumberGenerator.setShallHalt(true);
            evenNumberGenerator.setShallHalt(true);
        }else {
            System.out.println("Halting: daemon generator threads will be auto-cleaned");
        }
    }

    public void runWithAtomicIntegerLockSnippet(){
        logger.info("Generator launched in mode: runWithAtomicIntegerLockSnippet");
        AtomicInteger ai = new AtomicInteger(0);
        // don't start the threads immediately
        oddNumberGenerator = new OddNumberGenerator(ai);
        Thread tOdd = new Thread(oddNumberGenerator);
        tOdd.setName("Th-Odd-Gen-cs0x65");
        evenNumberGenerator = new EvenNumberGenerator(ai);
        Thread tEven = new Thread(evenNumberGenerator);
        tEven.setName("Th-Even-Gen-cs0x65");
        tOdd.start();
        tEven.start();
    }

    public void runWithClassLockSnippet(){
        logger.info("Generator launched in mode: runWithClassLockSnippet");
        // don't start the threads immediately
        oddNumberGenerator = new OddNumberGenerator(OddEvenTurnByTurnGenerationSnippet.class);
        Thread tOdd = new Thread(oddNumberGenerator);
        tOdd.setName("Th-Odd-Gen-cs0x65");
        evenNumberGenerator = new EvenNumberGenerator(OddEvenTurnByTurnGenerationSnippet.class);
        Thread tEven = new Thread(evenNumberGenerator);
        tEven.setName("Th-Even-Gen-cs0x65");
        tOdd.start();
        tEven.start();
    }

    /**
     * With main thread no more ceasing to exist, generator daemon threads will be stop executing -
     * albeit it takes some time.
     */
    public void runWithAtomicIntegerLockAndDaemonModeSnippet(){
        logger.info("Generator launched in mode: runWithAtomicIntegerLockAndDaemonModeSnippet");
        AtomicInteger ai = new AtomicInteger(0);
        // don't start the threads immediately
        oddNumberGenerator = new OddNumberGenerator(ai);
        Thread tOdd = new Thread(oddNumberGenerator);
        tOdd.setName("Th-Odd-Gen-cs0x65");
        tOdd.setDaemon(true);
        evenNumberGenerator = new EvenNumberGenerator(ai);
        Thread tEven = new Thread(evenNumberGenerator);
        tEven.setName("Th-Even-Gen-cs0x65");
        tEven.setDaemon(true);
        tOdd.start();
        tEven.start();
    }
}

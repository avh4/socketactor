package net.avh4.util;

/**
 * Provides a testable interface for writing `Runnable`s that will loop continuously in a thread.
 * <p/>
 * Lifecycle of a loop
 * <p/>
 * - `prepare()` is called once
 * - if `prepare()` throws an exception, `exception(e)` will  be called, followed by `cleanup()`, and the thread will terminate
 * - otherwise, `loop()` will be called repeatedly until it returns false, throws an exception, or the thread is interrupted
 * - if the thread was interrupted, `interrupted()` will be called
 * - if `loop()` threw an exception, `exception(e)` will be called
 * - in either case, `cleanup()` will be called
 *
 * @see LoopRunnable
 */
public interface Loop {
    void prepare() throws Exception;

    boolean loop() throws Exception;

    void interrupted();

    void exception(Exception e);

    void cleanup();
}

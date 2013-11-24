package net.avh4.util.socket;

import net.avh4.util.Loop;
import net.avh4.util.LoopRunnable;

import java.io.IOException;
import java.nio.channels.Selector;

public class SelectorLoop implements Loop {
    private static int spawnedThreads = 0;

    public interface Delegate {
        void selected();

        void closed();
    }

    private final Selector selector;
    private final Delegate delegate;

    public static Runnable newRunnable(Selector selector, Delegate delegate) {
        return new LoopRunnable(new SelectorLoop(selector, delegate));
    }

    public static Thread newThread(Selector selector, Delegate delegate) {
        final String threadName = SelectorLoop.class.getSimpleName() + "-" + (++spawnedThreads);
        return new Thread(newRunnable(selector, delegate), threadName);
    }

    public SelectorLoop(Selector selector, Delegate delegate) {
        this.selector = selector;
        this.delegate = delegate;
    }

    @Override public void prepare() throws Exception {
    }

    @Override public boolean loop() throws Exception {
        if (!selector.isOpen()) {
            delegate.closed();
            return false;
        }
        selector.select();
        delegate.selected();
        return true;
    }

    @Override public void interrupted() {
    }

    @Override public void exception(Exception e) {
    }

    @Override public void cleanup() {
        try {
            selector.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

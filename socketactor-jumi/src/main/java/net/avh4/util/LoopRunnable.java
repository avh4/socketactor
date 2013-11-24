package net.avh4.util;

public class LoopRunnable implements Runnable {

    private final Loop loop;

    public LoopRunnable(Loop loop) {
        this.loop = loop;
    }

    @Override public void run() {
        try {
            loop.prepare();
            while (!Thread.interrupted()) {
                final boolean keepGoing = loop.loop();
                if (!keepGoing) return;
            }
            loop.interrupted();
        } catch (Exception e) {
            loop.exception(e);
        } finally {
            loop.cleanup();
        }
    }
}

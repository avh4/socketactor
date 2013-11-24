package net.avh4.util.socket.jumi;

import fi.jumi.actors.ActorRef;

import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

import static net.avh4.util.socket.jumi.LineBuffer.Listener;

/**
 * Implementation of `Socket.Listener` that will collect data in a buffer, and split it into lines.
 * It will send one line for every call of `next()` that it receives.  You must call `next()` to receive the first line,
 * and again for every subsequent line that you want to receive.  Note that message order for actors is not
 * guaranteed, so to guarantee ordering of the lines you should not call `next()` again until you receive a line.
 * If you do not care about ordering, use `LineBuffer` instead.
 */
public class SynchronizedLineBuffer implements Socket.Listener {

    private final Charset charset;
    private String buffer = "";
    private Queue<String> readyLines = new ArrayDeque<>();
    private int requests = 0;

    public SynchronizedLineBuffer(ActorRef<Listener> listener) {
        this(listener, Charset.forName("UTF-8"));
    }

    public SynchronizedLineBuffer(ActorRef<Listener> listener, Charset charset) {
        this.listener = listener;
        this.charset = charset;
    }

    private final ActorRef<Listener> listener;

    public void next() {
        requests++;
        fulfillRequests();
    }

    @Override public void received(byte[] data) {
        String dataString = buffer + new String(data, charset);
        final String[] lines = dataString.split("\n");

        int fullLines = lines.length;
        if (dataString.charAt(dataString.length() - 1) != '\n') {
            fullLines -= 1;
            buffer = lines[lines.length - 1];
        }
        readyLines.addAll(Arrays.asList(lines).subList(0, fullLines));
        fulfillRequests();
    }

    @Override public void disconnected(Throwable cause) {
        listener.tell().disconnected(cause);
    }

    private void fulfillRequests() {
        while (requests > 0 && !readyLines.isEmpty()) {
            listener.tell().receivedLine(readyLines.remove());
        }
    }
}

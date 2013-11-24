package net.avh4.util.socket.jumi;

import fi.jumi.actors.ActorRef;

import java.nio.charset.Charset;

/**
 * Implementation of `Socket.Listener` that will collect data in a buffer, and will send the next line from the buffer
 * whenever a newline is encountered.  Note that since message order is not guaranteed, lines may arrive out of
 * order.  If you need to guarantee order, use `SynchronizedLineBuffer`.
 */
public class LineBuffer implements Socket.Listener {

    public interface Listener {
        void receivedLine(String line);

        void disconnected(Throwable cause);
    }

    private final Charset charset;
    private String buffer = "";

    public LineBuffer(ActorRef<Listener> listener) {
        this(listener, Charset.forName("UTF-8"));
    }

    public LineBuffer(ActorRef<Listener> listener, Charset charset) {
        this.listener = listener;
        this.charset = charset;
    }

    private final ActorRef<Listener> listener;

    @Override public void received(byte[] data) {
        String dataString = buffer + new String(data, charset);
        final String[] lines = dataString.split("\n");

        int fullLines = lines.length;
        if (dataString.charAt(dataString.length() - 1) != '\n') {
            fullLines -= 1;
            buffer = lines[lines.length - 1];
        }
        for (int i = 0; i < fullLines; i++) {
            String line = lines[i];
            listener.tell().receivedLine(line);
        }
    }

    @Override public void disconnected(Throwable cause) {
        listener.tell().disconnected(cause);
    }
}

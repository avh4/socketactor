package net.avh4.util.socket.jumi;

import fi.jumi.actors.ActorRef;

import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

public class LinesSocketChannelReader extends SocketChannelReader<LineListener> {
    private final Charset charset;
    private final Queue<String> readyLines = new ArrayDeque<>();
    private String stringBuffer = "";

    LinesSocketChannelReader(SocketChannelActor socketChannelActor, int readBufferSize, SocketChannel channel) {
        this(socketChannelActor, readBufferSize, channel, Charset.forName("UTF-8"));
    }

    LinesSocketChannelReader(SocketChannelActor socketChannelActor, int readBufferSize, SocketChannel channel, Charset charset) {
        super(socketChannelActor, readBufferSize, channel);
        this.charset = charset;
    }

    protected void sendData() {
        byte[] data;
        synchronized (buffer) {
            buffer.flip();
            data = new byte[buffer.remaining()];
            buffer.get(data);
            buffer.clear();
        }

        String dataString = stringBuffer + new String(data, charset);
        final String[] lines = dataString.split("\n");

        int fullLines = lines.length;
        if (dataString.charAt(dataString.length() - 1) != '\n') {
            fullLines -= 1;
            stringBuffer = lines[lines.length - 1];
        } else {
            stringBuffer = "";
        }
        readyLines.addAll(Arrays.asList(lines).subList(0, fullLines));

        drainImplementationBuffer();
    }

    @Override protected void drainImplementationBuffer() {
        while (requestCount > 0 && !readyLines.isEmpty()) {
            requestCount--;
            receiver.tell().receivedLine(readyLines.remove());
        }
    }
}

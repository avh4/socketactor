package net.avh4.util.socket.jumi;

import fi.jumi.actors.ActorRef;
import net.avh4.util.socket.SelectorLoop;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class SocketChannelActor implements Socket {
    public static final int READ_BUFFER_SIZE = 1024 * 8;
    public static final int WRITE_BUFFER_SIZE = 256;

    private final String host;
    private final int port;
    private final ReaderFactory readerFactory;
    private final ByteBuffer writeBuffer;
    private final ActorRef<? extends Disconnectable> listener;

    private SocketChannel channel;
    private Selector selector;
    private SocketChannelReader reader;
    private Thread readerThread;

    private interface ReaderFactory {
        SocketChannelReader get(SocketChannelActor self, SocketChannel channel);
    }

    public static SocketChannelActor bytesSocketChannelActor(String host, int port, final ActorRef<BytesListener> listener) {
        return new SocketChannelActor(host, port, WRITE_BUFFER_SIZE, new ReaderFactory() {
            @Override public SocketChannelReader get(SocketChannelActor self, SocketChannel channel) {
                return new BytesSocketChannelReader(self, listener, READ_BUFFER_SIZE, channel);
            }
        }, listener);
    }

    public static SocketChannelActor linesSocketChannelActor(String host, int port, final ActorRef<LineListener> listener) {
        final int readBufferSize = READ_BUFFER_SIZE;
        return new SocketChannelActor(host, port, WRITE_BUFFER_SIZE, new ReaderFactory() {
            @Override public SocketChannelReader get(SocketChannelActor self, SocketChannel channel) {
                return new LinesSocketChannelReader(self, listener, readBufferSize, channel);
            }
        }, listener);
    }

    protected SocketChannelActor(String host, int port, int writeBufferSize, ReaderFactory readerFactory, ActorRef<? extends Disconnectable> listener) {
        this.host = host;
        this.port = port;
        this.readerFactory = readerFactory;
        this.listener = listener;
        this.writeBuffer = ByteBuffer.allocateDirect(writeBufferSize);
    }

    @Override public synchronized void connect() {
        SocketAddress address = new InetSocketAddress(host, port);
        try {
            selector = Selector.open();

            channel = SocketChannel.open(address);
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ);

            reader = readerFactory.get(this, channel);
            readerThread = SelectorLoop.newThread(selector, reader);
            readerThread.start();
            next();
        } catch (IOException e) {
            disconnect(e);
        }
    }

    @Override public synchronized void write(byte[] data) {
        if (channel == null) {
            disconnect("Not connected");
            return;
        }
        writeBuffer.put(data);
        writeBuffer.flip();
        try {
            final int bytesWritten = channel.write(writeBuffer);
            writeBuffer.compact();
            if (bytesWritten < data.length) {
                // This can be handled better by retrying the remainder of the buffer until a -1
                disconnect("Failed to write all bytes to the socket (" + bytesWritten + " of " + data.length + ")");
                //noinspection UnnecessaryReturnStatement
                return;
            }
        } catch (IOException e) {
            disconnect(e);
        }
    }

    @Override public void next() {
        reader.next();
    }

    protected synchronized void disconnect(String cause) {
        disconnect(new IOException(cause));
    }

    protected synchronized void disconnect(Throwable cause) {
        listener.tell().disconnected(cause);
        stopReaderThread();
        dropChannel();
        closeSelector();
        writeBuffer.clear();
    }

    private void stopReaderThread() {
        if (readerThread == null) return;
        readerThread.interrupt();
    }

    private synchronized void dropChannel() {
        if (channel == null) return;
        try {
            channel.close();
        } catch (IOException ignored) {
        }
        channel = null;
    }

    private void closeSelector() {
        if (selector == null) return;
        try {
            selector.close();
        } catch (IOException ignored) {
        }
        selector = null;
    }
}

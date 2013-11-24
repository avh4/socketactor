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
    private final String host;
    private final int port;
    private final ActorRef<Socket.Listener> listener;
    private final int readBufferSize;
    private volatile SocketChannel channel;
    private Thread readerThread;
    private Selector selector;

    public SocketChannelActor(String host, int port, ActorRef<Listener> listener) {
        this(host, port, listener, 256);
    }

    public SocketChannelActor(String host, int port, ActorRef<Listener> listener, int readBufferSize) {
        this.host = host;
        this.port = port;
        this.listener = listener;
        this.readBufferSize = readBufferSize;
    }

    public synchronized void connect() {
        SocketAddress address = new InetSocketAddress(host, port);
        try {
            selector = Selector.open();

            channel = SocketChannel.open(address);
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ);

            SelectorLoop.Delegate reader =
                    new SocketChannelReader(this, listener, readBufferSize, channel);
            readerThread = SelectorLoop.newThread(selector, reader);
            readerThread.start();
        } catch (IOException e) {
            disconnect(e);
        }
    }

    @Override public synchronized void write(byte[] data) {
        if (channel == null) {
            disconnect("Not connected");
            return;
        }
        ByteBuffer buffer = ByteBuffer.allocate(data.length);
        buffer.put(data);
        buffer.flip();
        try {
            final int bytesWritten = channel.write(buffer);
            // Should clear or compact the buffer, but we're going to throw it away
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

    protected synchronized void disconnect(String cause) {
        disconnect(new IOException(cause));
    }

    protected synchronized void disconnect(Throwable cause) {
        listener.tell().disconnected(cause);
        stopReaderThread();
        dropChannel();
        closeSelector();
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

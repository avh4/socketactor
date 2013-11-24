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

public class SocketChannelActor<L extends Disconnectable> implements Socket<L> {
    public static final int READ_BUFFER_SIZE = 1024 * 8;
    public static final int WRITE_BUFFER_SIZE = 256;

    private final String host;
    private final int port;
    private final ReaderFactory<L> readerFactory;
    private final ByteBuffer writeBuffer;

    private SocketChannel channel;
    private Selector selector;
    private SocketChannelReader<L> reader;
    private Thread readerThread;

    private interface ReaderFactory<L extends Disconnectable> {
        SocketChannelReader<L> get(SocketChannelActor self, SocketChannel channel);
    }

    public static SocketChannelActor<BytesListener> bytesSocketChannelActor(String host, int port) {
        return new SocketChannelActor<>(host, port, WRITE_BUFFER_SIZE, new ReaderFactory<BytesListener>() {
            @Override public SocketChannelReader<BytesListener> get(SocketChannelActor self, SocketChannel channel) {
                return new BytesSocketChannelReader(self, READ_BUFFER_SIZE, channel);
            }
        });
    }

    public static SocketChannelActor<LineListener> linesSocketChannelActor(String host, int port) {
        final int readBufferSize = READ_BUFFER_SIZE;
        return new SocketChannelActor<>(host, port, WRITE_BUFFER_SIZE, new ReaderFactory<LineListener>() {
            @Override public SocketChannelReader<LineListener> get(SocketChannelActor self, SocketChannel channel) {
                return new LinesSocketChannelReader(self, readBufferSize, channel);
            }
        });
    }

    protected SocketChannelActor(String host, int port, int writeBufferSize, ReaderFactory<L> readerFactory) {
        this.host = host;
        this.port = port;
        this.readerFactory = readerFactory;
        this.writeBuffer = ByteBuffer.allocateDirect(writeBufferSize);
    }

    @Override public synchronized void connect(ActorRef<L> initialReceiver) {
        SocketAddress address = new InetSocketAddress(host, port);
        try {
            selector = Selector.open();

            channel = SocketChannel.open(address);
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ);

            reader = readerFactory.get(this, channel);
            readerThread = SelectorLoop.newThread(selector, reader);
            readerThread.start();
            next(initialReceiver);
        } catch (IOException e) {
            disconnect(initialReceiver, e);
        }
    }

    @Override public synchronized void write(ActorRef<? extends Disconnectable> receiver, byte[] data) {
        if (channel == null) {
            disconnect(receiver, "Not connected");
            return;
        }
        writeBuffer.put(data);
        writeBuffer.flip();
        try {
            final int bytesWritten = channel.write(writeBuffer);
            writeBuffer.compact();
            if (bytesWritten < data.length) {
                // This can be handled better by retrying the remainder of the buffer until a -1
                disconnect(receiver, "Failed to write all bytes to the socket (" + bytesWritten + " of " + data.length + ")");
                //noinspection UnnecessaryReturnStatement
                return;
            }
        } catch (IOException e) {
            disconnect(receiver, e);
        }
    }

    @Override public void next() {
        reader.next();
    }

    @Override public void next(ActorRef<L> newReceiver) {
        reader.setReceiver(newReceiver);
        reader.next();
    }

    protected synchronized void disconnect(ActorRef<? extends Disconnectable> receiver, String cause) {
        disconnect(receiver, new IOException(cause));
    }

    protected synchronized void disconnect(ActorRef<? extends Disconnectable> receiver, Throwable cause) {
        receiver.tell().disconnected(cause);
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

package net.avh4.util.socket.jumi;

import fi.jumi.actors.ActorRef;

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
    private volatile SocketChannel channel;
    private Thread readerThread;

    public SocketChannelActor(String host, int port, ActorRef<Socket.Listener> listener) {
        this.host = host;
        this.port = port;
        this.listener = listener;
    }

    public synchronized void connect() {
        SocketAddress address = new InetSocketAddress(host, port);
        try {
            channel = SocketChannel.open(address);
            channel.configureBlocking(false);
            readerThread = new Thread(new ReaderThread(channel));
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

    private class ReaderThread implements Runnable {
        private final SocketChannel channel;

        private ReaderThread(SocketChannel channel) {
            this.channel = channel;
        }

        @Override public void run() {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(256);
                Selector sel = Selector.open();
                channel.register(sel, SelectionKey.OP_READ);
                while (!Thread.interrupted()) {
                    buffer.clear();
                    sel.select();
                    final int bytesRead = channel.read(buffer);
                    if (bytesRead == -1) {
                        disconnect("Socket was disconnected");
                        return;
                    }
                    buffer.flip();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    listener.tell().received(data);
                }
                disconnect("Reader Thread exited");
            } catch (Exception e) {
                disconnect(e);
            }
        }
    }
}

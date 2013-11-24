package net.avh4.util.socket.jumi;

import net.avh4.util.socket.SelectorLoop;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

abstract class SocketChannelReader implements SelectorLoop.Delegate {
    private final SocketChannel channel;
    private final SocketChannelActor socketChannelActor;
    protected final ByteBuffer buffer;
    protected int requestCount = 0;

    SocketChannelReader(SocketChannelActor socketChannelActor, int readBufferSize, SocketChannel channel) {
        this.channel = channel;
        this.socketChannelActor = socketChannelActor;
        buffer = ByteBuffer.allocateDirect(readBufferSize);
    }

    @Override public void selected() {
        final int bytesRead;
        synchronized (buffer) {
            try {
                bytesRead = channel.read(buffer);
            } catch (IOException e) {
                socketChannelActor.disconnect(e);
                return;
            }
        }

        if (bytesRead == -1) {
            socketChannelActor.disconnect("Socket was disconnected");
        }

        fulfillRequests();
    }

    @Override public void closed() {
        socketChannelActor.disconnect("Selector was closed");
    }

    public synchronized void next() {
        requestCount++;
        fulfillRequests();
    }

    private synchronized void fulfillRequests() {
        drainImplementationBuffer();
        synchronized (buffer) {
            if (requestCount > 0 && buffer.position() > 0) {
                sendData();
            }
        }
    }

    protected abstract void sendData();

    protected abstract void drainImplementationBuffer();
}

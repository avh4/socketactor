package net.avh4.util.socket.jumi;

import fi.jumi.actors.ActorRef;
import net.avh4.util.socket.SelectorLoop;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

abstract class SocketChannelReader<L extends Disconnectable> implements SelectorLoop.Delegate {
    private final SocketChannel channel;
    private final SocketChannelActor socketChannelActor;
    protected final ByteBuffer buffer;
    protected ActorRef<L> receiver;
    protected int requestCount = 0;
    private IOException queuedDisconnect;

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
                queueDisconnect(e);
                fulfillRequests();
                return;
            }
        }

        if (bytesRead == -1) {
            queueDisconnect("Socket was disconnected");
        }

        fulfillRequests();
    }

    @Override public void closed() {
        queueDisconnect("Selector was closed");
        fulfillRequests();
    }

    private synchronized  void queueDisconnect(String cause) {
        queueDisconnect(new IOException(cause));
    }

    private synchronized void queueDisconnect(IOException e) {
        if (queuedDisconnect != null) {
            throw new RuntimeException("Not implemented: disconnect was already queued--is this okay?");
        }
        queuedDisconnect = e;
    }

    public synchronized void next() {
        if (receiver == null) throw new RuntimeException("No receiver");
        requestCount++;
        if (requestCount > 1) throw new RuntimeException("Not implemented yet--need to be careful when switching receivers");
        fulfillRequests();
    }

    public synchronized void setReceiver(ActorRef<L> newReceiver) {
        this.receiver = newReceiver;
    }

    private synchronized void fulfillRequests() {
        drainImplementationBuffer();
        synchronized (buffer) {
            if (requestCount > 0) {
                if (queuedDisconnect != null) {
                    socketChannelActor.disconnect(receiver, queuedDisconnect);
                    queuedDisconnect = null;
                    requestCount--;
                    return;
                }
                if (buffer.position() > 0) {
                    sendData();
                }
            }
        }
    }

    protected abstract void sendData();

    protected abstract void drainImplementationBuffer();
}

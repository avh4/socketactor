package net.avh4.util.socket.jumi;

import fi.jumi.actors.ActorRef;

import java.nio.channels.SocketChannel;

public class BytesSocketChannelReader extends SocketChannelReader {
    private final ActorRef<BytesListener> listener;

    BytesSocketChannelReader(SocketChannelActor socketChannelActor, ActorRef<BytesListener> listener, int readBufferSize, SocketChannel channel) {
        super(socketChannelActor, readBufferSize, channel);
        this.listener = listener;
    }

    protected void sendData() {
        buffer.flip();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        listener.tell().received(data);
        requestCount = 0;
        buffer.clear();
    }

    @Override protected void drainImplementationBuffer() {
        // nothing to do
    }
}

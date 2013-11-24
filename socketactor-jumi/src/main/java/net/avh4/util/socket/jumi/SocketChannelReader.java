package net.avh4.util.socket.jumi;

import fi.jumi.actors.ActorRef;
import net.avh4.util.socket.SelectorLoop;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

class SocketChannelReader implements SelectorLoop.Delegate {
    private final SocketChannel channel;
    private final SocketChannelActor socketChannelActor;
    private final ActorRef<Socket.Listener> listener;
    private final ByteBuffer buffer;

    SocketChannelReader(SocketChannelActor socketChannelActor, ActorRef<Socket.Listener> listener, int readBufferSize, SocketChannel channel) {
        this.channel = channel;
        this.socketChannelActor = socketChannelActor;
        this.listener = listener;
        buffer = ByteBuffer.allocate(readBufferSize);
    }

    @Override public void selected() {
        buffer.clear();
        final int bytesRead;
        try {
            bytesRead = channel.read(buffer);
        } catch (IOException e) {
            socketChannelActor.disconnect(e);
            return;
        }

        if (bytesRead == -1) {
            socketChannelActor.disconnect("Socket was disconnected");
        }

        buffer.flip();
        if (buffer.remaining() > 0) {
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            listener.tell().received(data);
        }
    }

    @Override public void closed() {
        socketChannelActor.disconnect("Selector was closed");
    }
}

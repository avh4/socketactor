package net.avh4.util.socket.jumi;

import java.nio.channels.SocketChannel;

public class BytesSocketChannelReader extends SocketChannelReader<BytesListener> {
    BytesSocketChannelReader(SocketChannelActor socketChannelActor, int readBufferSize, SocketChannel channel) {
        super(socketChannelActor, readBufferSize, channel);
    }

    protected void sendData() {
        buffer.flip();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        receiver.tell().received(data);
        requestCount = 0;
        buffer.clear();
    }

    @Override protected void drainImplementationBuffer() {
        // nothing to do
    }
}

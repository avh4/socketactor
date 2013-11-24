package net.avh4.util.socket.jumi;

public interface BytesListener extends Disconnectable {
    void received(byte[] data);
}

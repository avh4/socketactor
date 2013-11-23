package net.avh4.util.socket.jumi;

public interface Socket {
    void connect();

    void write(byte[] data);

    interface Listener {
        void received(byte[] data);

        void disconnected(Throwable cause);
    }
}

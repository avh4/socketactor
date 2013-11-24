package net.avh4.util.socket.jumi;

public interface Socket {
    void connect();

    void write(byte[] data);

    void next();
}

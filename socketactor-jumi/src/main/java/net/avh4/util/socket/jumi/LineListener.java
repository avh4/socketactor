package net.avh4.util.socket.jumi;

public interface LineListener extends Disconnectable {
    void receivedLine(String line);
}

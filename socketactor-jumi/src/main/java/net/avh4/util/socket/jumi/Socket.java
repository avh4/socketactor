package net.avh4.util.socket.jumi;

import fi.jumi.actors.ActorRef;

public interface Socket<L extends Disconnectable> {
    void connect(ActorRef<L> initialReceiver);

    void write(ActorRef<? extends Disconnectable> receiver, byte[] data);

    void next();

    void next(ActorRef<L> newReceiver);
}

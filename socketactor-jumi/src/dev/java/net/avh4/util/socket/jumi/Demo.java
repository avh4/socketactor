package net.avh4.util.socket.jumi;

import fi.jumi.actors.ActorRef;
import fi.jumi.actors.ActorThread;
import fi.jumi.actors.Actors;
import fi.jumi.actors.MultiThreadedActors;
import fi.jumi.actors.eventizers.dynamic.DynamicEventizerProvider;
import fi.jumi.actors.listeners.CrashEarlyFailureHandler;
import fi.jumi.actors.listeners.PrintStreamMessageLogger;

import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Demo {
    public static final Charset UTF8 = Charset.forName("UTF-8");

    public static void main(String[] args) throws InterruptedException {
        ExecutorService threadPool = Executors.newCachedThreadPool();
        Actors actors = new MultiThreadedActors(threadPool,
                new DynamicEventizerProvider(),
                new CrashEarlyFailureHandler(),
                new PrintStreamMessageLogger(System.out));
        ActorThread actorThread = actors.startActorThread();

        ActorRef<Socket.Listener> listener = actorThread.bindActor(Socket.Listener.class, new Socket.Listener() {
            @Override public void received(byte[] data) {
                String s = new String(data, UTF8);
                System.out.println("RECEIVED: " + s);
            }

            @Override public void disconnected(Throwable cause) {
                System.out.println("DISCONNECTED: " + cause.getLocalizedMessage());
//                cause.printStackTrace();
            }
        });

        ActorRef<Socket> socket = actorThread.bindActor(Socket.class, new SocketChannelActor("192.168.2.33", 6600, listener));

        socket.tell().connect();
        socket.tell().write("xx1\n".getBytes());
        socket.tell().write("xx2\n".getBytes());
        Thread.sleep(5000);
        socket.tell().write("xx3\n".getBytes());
        Thread.sleep(72000);
        socket.tell().write("xx4\n".getBytes());
        Thread.sleep(72000);
        socket.tell().write("listall\n".getBytes());
        Thread.sleep(10000);

        actorThread.stop();
        threadPool.shutdown();
    }
}

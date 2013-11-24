package net.avh4.util.socket.jumi;

import fi.jumi.actors.ActorRef;
import fi.jumi.actors.ActorThread;
import fi.jumi.actors.Actors;
import fi.jumi.actors.MultiThreadedActors;
import fi.jumi.actors.eventizers.dynamic.DynamicEventizerProvider;
import fi.jumi.actors.listeners.CrashEarlyFailureHandler;
import fi.jumi.actors.listeners.NullMessageListener;
import fi.jumi.actors.listeners.PrintStreamMessageLogger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Demo {
    private static final String host = "192.168.2.33";
    private static final int port = 6600;
    private static ActorRef<Socket> socket;

    public static void main(String[] args) throws InterruptedException {
        ExecutorService threadPool = Executors.newCachedThreadPool();
        Actors actors = new MultiThreadedActors(threadPool,
                new DynamicEventizerProvider(),
                new CrashEarlyFailureHandler(),
                new NullMessageListener());
//                new PrintStreamMessageLogger(System.out));
        ActorThread actorThread = actors.startActorThread();

        ActorRef<LineListener> lineBufferListener = actorThread.bindActor(LineListener.class,
                new LineListener() {
                    @Override public void receivedLine(String line) {
                        System.out.println("RECEIVED: " + line);
                        socket.tell().next();
                    }

                    @Override public void disconnected(Throwable cause) {
                        System.out.println("DISCONNECTED: " + cause.getLocalizedMessage());
//                cause.printStackTrace();
                    }
                });
        socket = actorThread.bindActor(Socket.class,
                SocketChannelActor.linesSocketChannelActor(host, port, lineBufferListener));

        socket.tell().connect();
        socket.tell().write("xx1\n".getBytes());
        socket.tell().write("xx2\n".getBytes());
        Thread.sleep(5000);
        socket.tell().write("listall\n".getBytes());
        Thread.sleep(72000);
        socket.tell().write("xx4\n".getBytes());
        Thread.sleep(10000);

        actorThread.stop();
        threadPool.shutdown();
    }
}

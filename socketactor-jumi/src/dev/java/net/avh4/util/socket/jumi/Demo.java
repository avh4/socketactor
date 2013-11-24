package net.avh4.util.socket.jumi;

import fi.jumi.actors.ActorRef;
import fi.jumi.actors.ActorThread;
import fi.jumi.actors.Actors;
import fi.jumi.actors.MultiThreadedActors;
import fi.jumi.actors.eventizers.dynamic.DynamicEventizerProvider;
import fi.jumi.actors.listeners.CrashEarlyFailureHandler;
import fi.jumi.actors.listeners.NullMessageListener;

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

        final MyLineListener rl1 = new MyLineListener("1");
        final MyLineListener rl2 = new MyLineListener("2");
        ActorRef<LineListener> listener1 = actorThread.bindActor(LineListener.class, rl1);
        ActorRef<LineListener> listener2 = actorThread.bindActor(LineListener.class, rl2);
        rl1.next = listener2;
        rl2.next = listener1;
        ActorRef<LineListener> listener3 = actorThread.bindActor(LineListener.class, new MyLineListener("3"));
        ActorRef<LineListener> listener4 = actorThread.bindActor(LineListener.class, new MyLineListener("4"));
        socket = actorThread.bindActor(Socket.class,
                SocketChannelActor.linesSocketChannelActor(host, port));

        socket.tell().connect(listener1);
        socket.tell().write(listener3, "xx1\n".getBytes());
        socket.tell().write(listener4, "xx2\n".getBytes());
        Thread.sleep(5000);
        socket.tell().write(listener3, "listall\n".getBytes());
        Thread.sleep(72000);
        socket.tell().write(listener4, "xx4\n".getBytes());
        Thread.sleep(10000);

        actorThread.stop();
        threadPool.shutdown();
    }

    private static class MyLineListener implements LineListener {
        private final String name;
        public ActorRef next;

        private MyLineListener(String name) {
            this.name = name;
        }

        @Override public void receivedLine(String line) {
            System.out.println(name + ": RECEIVED: " + line);
            if (next != null) {
                socket.tell().next(next);
            } else {
                socket.tell().next();
            }
        }

        @Override public void disconnected(Throwable cause) {
            System.out.println(name + ": DISCONNECTED: " + cause.getLocalizedMessage());
//                cause.printStackTrace();
        }
    }
}

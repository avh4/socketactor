[![Build Status](https://secure.travis-ci.org/avh4/socketactor.png?branch=master)](http://travis-ci.org/avh4/socketactor)

## socketactor-jumi

Provides `SocketChannelActor`, making it easy to create TCP clients/servers using the [jumi-actors framework](http://jumi.fi/actors.html).

### Usage

Add the following dependencies to your `pom.xml`:

**NOTE**: socketactor is not yet published on Maven central, so you will first need to clone and `mvn install` this project.

```xml
  <dependency>
    <groupId>net.avh4.util.socket</groupId>
    <artifactId>socketactor-jumi</artifactId>
    <version>0.0.0-SNAPSHOT</version>
  </dependency>
```

To create a TCP client, you will first need to initialize `jumi-actors`:

```java
    ExecutorService threadPool = Executors.newCachedThreadPool();
    Actors actors = new MultiThreadedActors(threadPool,
            new DynamicEventizerProvider(),
            new CrashEarlyFailureHandler(),
            new PrintStreamMessageLogger(System.out));
    ActorThread actorThread = actors.startActorThread();
```

Now you can create your `received` and `disconnected` callbacks: (If your TCP protocol is not line-based, use `BytesListener` instead of `LinesListener`.)

```java
    ActorRef<Socket> socket;
    ActorRef<LineListener> listener = actorThread.bindActor(LineListener.class,
      new LineListener() {
        @Override public void receivedLine(String line) {
          System.out.println("RECEIVED: " + line);
          socket.tell().next();
        }

        @Override public void disconnected(Throwable cause) {
          System.out.println("DISCONNECTED: " + cause.getLocalizedMessage());
        }
      });
```

Now connect your client to a server: (If your TCP protocol is not line-based, use `SocketChannelActor.bytesSocketChannelActor` instead of `linesSocketChannelActor`.)

```java
    socket = actorThread.bindActor(Socket.class,
      SocketChannelActor.linesSocketChannelActor(host, port, listener));

    socket.tell().connect();
    socket.tell().write("currentsong\n".getBytes());
```

## Build commands

* [Mutation coverage](http://pitest.org/): `mvn clean test org.pitest:pitest-maven:mutationCoverage`

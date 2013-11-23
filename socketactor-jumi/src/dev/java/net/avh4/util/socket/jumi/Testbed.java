package net.avh4.util.socket.jumi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

public class Testbed {
    public static void main(String[] args) {
        try {
            final InetSocketAddress address = new InetSocketAddress("192.168.2.33", 6600);
            SocketChannel channel = SocketChannel.open(address);
//            final InputStreamReaderThread target = new InputStreamReaderThread(channel);
            final ChannelReaderThread target = new ChannelReaderThread(channel);
            Thread reader = new Thread(target);
            reader.start();

            Thread.sleep(500);
            target.write("xcv\n");

            Thread.sleep(5000);
            target.write("xcv\n");

            Thread.sleep(62000);
            target.write("xcv\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class InputStreamReaderThread implements Runnable {
        private final OutputStream outputStream;
        private final InputStream inputStream;

        public InputStreamReaderThread(SocketChannel channel) throws IOException {
            Socket socket = channel.socket();
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
        }

        public void write(String data) throws IOException {
            System.out.println("WRITING");
            outputStream.write(data.getBytes());
        }

        @Override public void run() {
            System.out.println("READER STARTED");
            try {
                while (!Thread.interrupted()) {
                    long start = System.currentTimeMillis();
                    final int read = inputStream.read();
                    long end = System.currentTimeMillis();
                    System.out.println("READ: " + read + ": " + Character.toString((char) read) + "  (" + (end - start) / 1000.0 + "s)");
                    if (read == -1) {
                        System.out.println("READING ENDED");
                        return;
                    }
                }
            } catch (Exception e) {
                System.out.println("READER DIED WITH EXCEPTION: ");
                e.printStackTrace();
            }
        }
    }

    private static class ChannelReaderThread implements Runnable {
        private final SocketChannel channel;
        private final ByteBuffer writeBuffer;

        public ChannelReaderThread(SocketChannel channel) throws IOException {
            channel.configureBlocking(false);
            this.channel = channel;
            writeBuffer = ByteBuffer.allocate(256);
        }

        public void write(String data) throws IOException {
            System.out.println("WRITING");
            writeBuffer.clear();
            writeBuffer.put(data.getBytes());
            writeBuffer.flip();
            channel.write(writeBuffer);
        }

        @Override public void run() {
            System.out.println("READER STARTED");
            try {
                ByteBuffer buffer = ByteBuffer.allocate(256);
                Selector sel = Selector.open();
                channel.register(sel, SelectionKey.OP_READ);
                while (!Thread.interrupted()) {
                    long start = System.currentTimeMillis();
                    sel.select();
                    long wait = System.currentTimeMillis();
                    buffer.clear();
                    final int bytesRead = channel.read(buffer);
                    if (bytesRead == -1) {
                        System.out.println("READING ENDED");
                        return;
                    }
                    buffer.put((byte) 0);
                    long end = System.currentTimeMillis();
                    buffer.flip();
                    byte[] arr = new byte[buffer.remaining()];
                    buffer.get(arr);
                    String s = new String(arr, Charset.forName("UTF-8"));
                    System.out.println("READ: " + s + "  (" + (wait - start) / 1000.0 + "s, " + (end - wait) / 1000.0 + "s)");
                }
            } catch (Exception e) {
                System.out.println("READER DIED WITH EXCEPTION: ");
                e.printStackTrace();
            }
        }
    }
}

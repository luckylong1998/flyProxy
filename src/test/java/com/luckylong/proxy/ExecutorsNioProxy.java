package com.luckylong.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class ExecutorsNioProxy {
    private final String remoteHost;
    private final int remotePort;
    private final int localPort;
    private final ExecutorService threadPool;
    private final Semaphore connectionSemaphore;

    public ExecutorsNioProxy(String remoteHost, int remotePort, int localPort, int maxConnections) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
        this.localPort = localPort;
        this.threadPool = Executors.newFixedThreadPool(maxConnections);
        this.connectionSemaphore = new Semaphore(maxConnections);
    }

    public void start() throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(localPort));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            try {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (!key.isValid()) continue;

                    if (key.isAcceptable()) {
                        if (connectionSemaphore.tryAcquire()) {
                            threadPool.execute(() -> {
                                try {
                                    accept(key, selector);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                        } else {
                            System.out.println("Maximum connections reached. Connection refused.");
                            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                            SocketChannel clientChannel = serverSocketChannel.accept();
                            clientChannel.close();
                        }
                    } else if (key.isReadable()) {
                        threadPool.execute(() -> {
                            read(key, selector);
                        });
                    } else if (key.isWritable()) {
                        threadPool.execute(() -> {
                            try {
                                write(key);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    private void accept(SelectionKey key, Selector selector) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        if(clientChannel == null) {
            System.out.println("Failed to accept connection.");
            return;
        }

        clientChannel.configureBlocking(false);

        InetSocketAddress remoteAddress = (InetSocketAddress) clientChannel.getRemoteAddress();
        String clientIpAddress = remoteAddress.getAddress().getHostAddress();
        System.out.println("Accepted connection from " + clientIpAddress);

        SocketChannel remoteChannel = SocketChannel.open(new InetSocketAddress(remoteHost, remotePort));
        remoteChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ, remoteChannel);
        remoteChannel.register(selector, SelectionKey.OP_READ, clientChannel);
    }

    private void read(SelectionKey key, Selector selector) {
        SocketChannel channel = (SocketChannel) key.channel();
        SocketChannel peerChannel = (SocketChannel) key.attachment();

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        try {
            int bytesRead = channel.read(buffer);
            if (bytesRead == -1) {
                closeConnection(channel, peerChannel);
                return;
            }

            buffer.flip();

            String protocol = detectProtocol(buffer);
            System.out.println("Detected protocol: " + protocol);

            while (buffer.hasRemaining()) {
                peerChannel.write(buffer);
            }

            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            peerChannel.keyFor(selector).interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);

        } catch (IOException e) {
            if (e instanceof java.net.SocketException && e.getMessage().contains("Connection reset")) {
                System.out.println("Connection reset by peer.");
            } else {
                e.printStackTrace();
            }
            try {
                closeConnection(channel, peerChannel);
            } catch (IOException closeException) {
                closeException.printStackTrace();
            }
        }
    }


    private void write(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        channel.write(ByteBuffer.allocate(0));
        key.interestOps(SelectionKey.OP_READ);
    }

    private void closeConnection(SocketChannel channel, SocketChannel peerChannel) throws IOException {
        channel.close();
        peerChannel.close();
        connectionSemaphore.release();
    }

    private String detectProtocol(ByteBuffer buffer) {
        String protocol = "UNKNOWN";
        String data = new String(buffer.array(), buffer.position(), buffer.limit());

        if (data.startsWith("GET") || data.startsWith("POST") || data.startsWith("HEAD")) {
            protocol = "HTTP";
        } else if (data.startsWith("SSH-")) {
            protocol = "SSH";
        } else if (data.startsWith("\u0016\u0003\u0001") || data.startsWith("\u0016\u0003\u0003")) {
            protocol = "HTTPS";
        }
        // Add more protocol checks as needed

        return protocol;
    }

    public static void main(String[] args) throws IOException {
        ExecutorsNioProxy proxy = new ExecutorsNioProxy("172.18.1.50", 3000, 8080, 1000);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                proxy.shutdown();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
        proxy.start();
    }

    private void shutdown() throws IOException {
        threadPool.shutdownNow();
        System.out.println("退出");
    }
}

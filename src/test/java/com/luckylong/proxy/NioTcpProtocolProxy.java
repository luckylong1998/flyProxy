package com.luckylong.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class NioTcpProtocolProxy {
    private final int localPort;
    private final Map<String, InetSocketAddress> protocolMap;

    public NioTcpProtocolProxy(int localPort) {
        this.localPort = localPort;
        this.protocolMap = new HashMap<>();
        initializeProtocolMap();
    }

    private void initializeProtocolMap() {
        protocolMap.put("HTTP", new InetSocketAddress("172.18.1.50", 3000));
        protocolMap.put("SSH", new InetSocketAddress("172.18.1.50", 22));
        // Add more protocol mappings as needed
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
                        accept(key, selector);
                    } else if (key.isReadable()) {
                        read(key, selector);
                    } else if (key.isWritable()) {
                        write(key);
                    } else if (key.isConnectable()) {
                        connect(key);
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
        clientChannel.configureBlocking(false);

        InetSocketAddress remoteAddress = (InetSocketAddress) clientChannel.getRemoteAddress();
        String clientIpAddress = remoteAddress.getAddress().getHostAddress();
        System.out.println("Accepted connection from " + clientIpAddress);

        // Register the client channel with an initial interest in reading.
        clientChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(1024));
    }

    private void read(SelectionKey key, Selector selector) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        buffer.clear();
        int bytesRead = clientChannel.read(buffer);
        if (bytesRead == -1) {
            clientChannel.close();
            return;
        }

        buffer.flip();

        String protocol = detectProtocol(buffer);
        System.out.println("Detected protocol: " + protocol);

        InetSocketAddress targetAddress = protocolMap.get(protocol);
        if (targetAddress == null) {
            System.out.println("No target address for protocol: " + protocol);
            clientChannel.close();
            return;
        }

        SocketChannel remoteChannel = SocketChannel.open();
        remoteChannel.configureBlocking(false);
        boolean connected = remoteChannel.connect(targetAddress);

        if (connected) {
            System.out.println("Connected immediately to " + targetAddress);
            remoteChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(1024));
            clientChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, remoteChannel);
        } else {
            System.out.println("Connecting to " + targetAddress);
            remoteChannel.register(selector, SelectionKey.OP_CONNECT, clientChannel);
            clientChannel.register(selector, SelectionKey.OP_READ, remoteChannel);
        }
    }

    private void write(SelectionKey key) throws IOException {
        System.out.println("Entering write method");
        SocketChannel channel = (SocketChannel) key.channel();
        SocketChannel peerChannel = (SocketChannel) key.attachment();
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        int bytesRead = peerChannel.read(buffer);
        if (bytesRead == -1) {
            System.out.println("Closing channels due to end of stream");
            channel.close();
            peerChannel.close();
            return;
        }

        buffer.flip();
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }

        System.out.println("Wrote data to channel");

        // Reset interestOps to read
        key.interestOps(SelectionKey.OP_READ);
        peerChannel.keyFor(key.selector()).interestOps(SelectionKey.OP_READ);
    }

    private void connect(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        SocketChannel peerChannel = (SocketChannel) key.attachment();

        if (channel.finishConnect()) {
            System.out.println("Finished connecting to remote host");
            channel.register(key.selector(), SelectionKey.OP_READ, peerChannel);
            peerChannel.keyFor(key.selector()).interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        } else {
            System.out.println("Connection failed");
            key.cancel();
            channel.close();
        }
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
        NioTcpProtocolProxy proxy = new NioTcpProtocolProxy(8080);
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
        // Implement graceful shutdown logic if needed
    }
}

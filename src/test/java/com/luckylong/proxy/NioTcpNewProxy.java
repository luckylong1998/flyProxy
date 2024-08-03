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

public class NioTcpNewProxy {
    private final String remoteHost;
    private final int remotePort;
    private final int localPort;

    public NioTcpNewProxy(String remoteHost, int remotePort, int localPort) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
        this.localPort = localPort;
    }

    public void start() throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(localPort));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        ByteBuffer buffer = ByteBuffer.allocate(1024);

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
                        System.out.println("接受");
                        accept(key, selector);
                    } else if (key.isReadable()) {
                        System.out.println("读取");
                        read(key, selector, buffer);
                    } else if (key.isWritable()) {
                        System.out.println("写入");
                        write(key);
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


        SocketChannel remoteChannel = SocketChannel.open(new InetSocketAddress(remoteHost, remotePort));
        remoteChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ, remoteChannel);
        remoteChannel.register(selector, SelectionKey.OP_READ, clientChannel);
    }

    private void read(SelectionKey key, Selector selector, ByteBuffer buffer) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        SocketChannel peerChannel = (SocketChannel) key.attachment();

        buffer.clear();
        int bytesRead = channel.read(buffer);
        if (bytesRead == -1) {
            channel.close();
            peerChannel.close();
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
    }

    private void write(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        channel.write(ByteBuffer.allocate(0));
        key.interestOps(SelectionKey.OP_READ);
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
        NioTcpNewProxy proxy = new NioTcpNewProxy("172.18.1.50", 3000, 8080);
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
        System.out.println("退出");
    }
}

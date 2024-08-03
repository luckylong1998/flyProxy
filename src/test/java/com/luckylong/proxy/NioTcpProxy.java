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

public class NioTcpProxy {
    private final String remoteHost;
    private final int remotePort;
    private final int localPort;

    public NioTcpProxy(String remoteHost, int remotePort, int localPort) {
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
        while (true) {
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
                }
            }
        }
    }

    private void accept(SelectionKey key, Selector selector) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        SocketChannel remoteChannel = SocketChannel.open(new InetSocketAddress(remoteHost, remotePort));
        remoteChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ, remoteChannel);
        remoteChannel.register(selector, SelectionKey.OP_READ, clientChannel);
    }

    private void read(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        SocketChannel peerChannel = (SocketChannel) key.attachment();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead = channel.read(buffer);
        if (bytesRead == -1) {
            channel.close();
            peerChannel.close();
            return;
        }
        buffer.flip();
        peerChannel.write(buffer);
        key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        peerChannel.keyFor(selector).interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }

    private void write(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        channel.write(ByteBuffer.allocate(0));
        key.interestOps(SelectionKey.OP_READ);
    }

    public static void main(String[] args) throws IOException {
        new NioTcpProxy("172.18.1.50", 3000, 8080).start();
    }
}

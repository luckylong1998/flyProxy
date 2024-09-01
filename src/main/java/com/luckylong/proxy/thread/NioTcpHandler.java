package com.luckylong.proxy.thread;

import com.luckylong.proxy.config.Config;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class NioTcpHandler {
    private final Config config;

    public NioTcpHandler(Config config) {
        this.config = config;
    }

    public void start() throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(config.getPort()));
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

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isAcceptable()) {
                        accept(key, selector);
                    } else if (key.isReadable()) {
                        read(key, selector, buffer);
                    } else if (key.isWritable()) {
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

        String forward = config.getIpForward().getForward(clientIpAddress);
        System.out.println(" remote: " + forward);
        String remoteIp = forward.split(":")[0];
        int remotePort = Integer.parseInt( forward.split(":")[1]);

        SocketChannel remoteChannel = SocketChannel.open(new InetSocketAddress(remoteIp, remotePort));
        remoteChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ, remoteChannel);
        remoteChannel.register(selector, SelectionKey.OP_READ, clientChannel);
    }

    private void read(SelectionKey key, Selector selector, ByteBuffer buffer) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        SocketChannel peerChannel = (SocketChannel) key.attachment();
        String clientIp = ((InetSocketAddress) channel.getRemoteAddress()).getAddress().getHostAddress();
        buffer.clear();
        try {
            int bytesRead = channel.read(buffer);
            if (bytesRead == -1) {
                channel.close();
                peerChannel.close();
                return;
            }
        } catch (Exception e) {
            channel.close();
            peerChannel.close();
            if(!"Connection reset".equals(e.getMessage())){
                e.printStackTrace();
                return;
            }
            System.out.println("Client IP: " + clientIp + " close connection");
            return;
        }


        buffer.flip();


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



    public static void main(String[] args) throws IOException {
        //NioTcpHandler proxy = new NioTcpHandler("172.18.1.50", 22, 8080);
        //Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        //    try {
        //        proxy.shutdown();
        //    } catch (IOException e) {
        //        e.printStackTrace();
        //    }
        //}));
        //proxy.start();
    }

    public void shutdown() throws IOException {
        // Implement graceful shutdown logic if needed
        System.out.println(" shutdown ");
    }
}

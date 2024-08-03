/**
 * Copyright 2020-2030 luckylong1998@163.com(https://gitee.com/luckylong1998)(https://github.com/luckylong1998)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.luckylong.proxy.thread;

import com.luckylong.proxy.config.Config;
import com.luckylong.proxy.util.FileUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URISyntaxException;

/**
 * 客户端请求类
 * @author xiaofeilong
 * @date 2024/8/3 16:53
 */
public class ClientHandler extends Thread {

    private final Socket socket;

    //private static final int SOCKET_TIMEOUT_MS = 500000;


    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {

        System.out.println("Handling new connection from " + socket.getInetAddress() + ":" + socket.getPort());
        try (InputStream input = socket.getInputStream()) {
            //socket.setSoTimeout(SOCKET_TIMEOUT_MS); // Set a timeout to avoid blocking indefinitely

            byte[] buffer = new byte[8];

            int bytesRead = input.read(buffer);
            if (bytesRead != -1) {
                //是否开启IP转发
                Config config = FileUtil.getConfig();
               if(config.getIpForward().isEnable()){
                   String forward = config.getIpForward().getForward(socket.getInetAddress().getHostAddress());
                   handleTraffic(socket, buffer, bytesRead, forward);
               } else if(config.getClientProtocolForward().isEnable()){
                   String forward = config.getClientProtocolForward().getForward(buffer, bytesRead);
                   handleTraffic(socket, buffer, bytesRead, forward);
               }
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }finally {
            System.out.println("Connection from " + socket.getInetAddress() + ":" + socket.getPort() + " handled.");
        }
    }

    /**
     * 处理转发
     * @author xiaofeilong
     * @date 2024/8/3 17:42
     * @param [socket, buffer, bytesRead, forward]
     * @return void
     */
    private void handleTraffic(Socket socket, byte[] buffer, int bytesRead, String forward) throws URISyntaxException {
        String host = forward.split(":")[0];
        int port = Integer.parseInt(forward.split(":")[1]);
        try (Socket ipBackend = new Socket(host, port)) {
            relayTraffic(socket, ipBackend, buffer, bytesRead);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 处理流量信息
     * @author xiaofeilong
     * @date 2024/8/3 17:41
     * @param [clientSocket, backendSocket, initialBuffer, initialBytesRead]
     * @return void
     */
    private void relayTraffic(Socket clientSocket, Socket backendSocket, byte[] initialBuffer, int initialBytesRead) {
        try (InputStream clientInput = clientSocket.getInputStream();
             OutputStream clientOutput = clientSocket.getOutputStream();
             InputStream backendInput = backendSocket.getInputStream();
             OutputStream backendOutput = backendSocket.getOutputStream()) {

            // Send the initial payload to the backend
            backendOutput.write(initialBuffer, 0, initialBytesRead);
            backendOutput.flush();

            Thread clientToBackend = new Thread(() -> {
                try {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = clientInput.read(buffer)) != -1) {
                        backendOutput.write(buffer, 0, bytesRead);
                        backendOutput.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Connection to backend lost: " + e.getMessage());
                }
            });

            Thread backendToClient = new Thread(() -> {
                try {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = backendInput.read(buffer)) != -1) {
                        clientOutput.write(buffer, 0, bytesRead);
                        clientOutput.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Connection to client lost: " + e.getMessage());
                }
            });

            clientToBackend.start();
            backendToClient.start();

            clientToBackend.join();
            backendToClient.join();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.out.println("Error during traffic relay: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                backendSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Error closing sockets: " + e.getMessage());
            }
        }
    }

}

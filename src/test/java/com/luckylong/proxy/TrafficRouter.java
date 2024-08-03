package com.luckylong.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrafficRouter {

    private static final int PORT = 8080;
    private static final String HTTP_HEADER = "HTTP/";
    private static final String SSH_HEADER = "SSH-2.0";
    private static final int THREAD_POOL_SIZE = 8;

    public static void main(String[] args) {
        ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                threadPool.submit(new ClientHandler(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            System.out.println("Handling new connection from " + socket.getInetAddress() + ":" + socket.getPort());
            try (InputStream input = socket.getInputStream()) {
                byte[] buffer = new byte[8];
                int bytesRead = input.read(buffer);

                if (bytesRead != -1) {
                    String payload = new String(buffer, 0, bytesRead);
                    if (payload.startsWith(HTTP_HEADER)) {
                        handleHttpTraffic(socket, buffer, bytesRead);
                    } else if (payload.startsWith(SSH_HEADER)) {
                        handleSshTraffic(socket, buffer, bytesRead);
                    } else {
                        handleDefaultTraffic(socket, buffer, bytesRead);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                System.out.println("Connection from " + socket.getInetAddress() + ":" + socket.getPort() + " handled.");
            }
        }

        private void handleHttpTraffic(Socket socket, byte[] buffer, int bytesRead) {
            try (Socket httpBackend = new Socket("172.18.1.50", 80)) {
                relayTraffic(socket, httpBackend, buffer, bytesRead);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void handleSshTraffic(Socket socket, byte[] buffer, int bytesRead) {
            try (Socket sshBackend = new Socket("172.18.1.50", 22)) {
                relayTraffic(socket, sshBackend, buffer, bytesRead);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void handleDefaultTraffic(Socket socket, byte[] buffer, int bytesRead) {
            try (Socket defaultBackend = new Socket("172.18.1.50", 3000)) {
                relayTraffic(socket, defaultBackend, buffer, bytesRead);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

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
                    }
                });

                clientToBackend.start();
                backendToClient.start();

                clientToBackend.join();
                backendToClient.join();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                    backendSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

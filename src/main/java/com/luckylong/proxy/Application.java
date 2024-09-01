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
package com.luckylong.proxy;

import com.luckylong.proxy.config.Config;
import com.luckylong.proxy.thread.ClientHandler;
import com.luckylong.proxy.util.FileUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 项目启动类
 * @author xiaofeilong
 * @date 2024/8/3 15:29
 */
public class Application {

    public static void main(String[] args) {
        Config config = FileUtil.getConfig();

        ExecutorService threadPool = Executors.newFixedThreadPool(config.getThreadPoolSize());

        try (ServerSocket serverSocket = new ServerSocket(config.getPort())) {
            System.out.println("Server is listening on port " + config.getPort());
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
}

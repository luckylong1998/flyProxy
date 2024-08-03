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
package com.luckylong.proxy.config;

import java.io.Serial;
import java.io.Serializable;

/**
 * 配置文件
 * @author xiaofeilong
 * @date 2024/8/3 15:50
 */
public class Config implements Serializable {

    @Serial
    private static final long serialVersionUID = -5965185328464672619L;

    // 程序端口号
    private int port;

    //ip转发配置
    private IpForward ipForward;

    //客户端转发配置
    private ClientProtocolForward clientProtocolForward;

    //线程池大小
    private int threadPoolSize;

    //默认转发规则
    private String defaultForward;

    // Getters and Setters  
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public IpForward getIpForward() {
        return ipForward;
    }

    public void setIpForward(IpForward ipForward) {
        this.ipForward = ipForward;
    }

    public ClientProtocolForward getClientProtocolForward() {
        return clientProtocolForward;
    }

    public void setClientProtocolForward(ClientProtocolForward clientProtocolForward) {
        this.clientProtocolForward = clientProtocolForward;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public String getDefaultForward() {
        return defaultForward;
    }

    public void setDefaultForward(String defaultForward) {
        this.defaultForward = defaultForward;
    }

    public Config() {

    }

    public Config(int port, IpForward ipForward, ClientProtocolForward clientProtocolForward, int threadPoolSize, String defaultForward) {
        this.port = port;
        this.ipForward = ipForward;
        this.clientProtocolForward = clientProtocolForward;
        this.threadPoolSize = threadPoolSize;
        this.defaultForward = defaultForward;
    }
}
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
package com.luckylong.proxy.util;

/**
 * 客户端协议工具类
 * @author xiaofeilong
 * @date 2024/8/3 18:05
 */
public class ProtocolUtil {


    private static final String HTTP_HEADER = "HTTP/";

    private static final String GET = "GET";

    private static final String POST = "POST";

    private static final String PUT = "PUT";

    private static final String DELETE = "DELETE";

    private static final String SSH_HEADER = "SSH-2.0";

    private static final String FTP_HEADER = "220";

    private static final String HTTP = "http";

    private static final String HTTPS = "https";

    private static final String SSH = "ssh";

    private static final String MYSQL = "mysql";

    private static final String FTP = "ftp";



    //HTTP：通常以 HTTP/  GET  PUT  DELETE  POST  开头。
    //HTTPS：以 TLS 握手开始，特征字节为 0x16 开头，并且第二个字节是 0x03。
    //SSH：通常以 SSH-2.0 开头。
    //MySQL：通常以 0x10 开头并且有一个特定的长度字段。
    //FTP：响应以 220 开头表示服务准备好。

    public static String getProtocol(byte[] buffer, int bytesRead) {
        String payload = new String(buffer, 0, bytesRead);
        System.out.println("协议：" + payload);
        if (payload.startsWith(HTTP_HEADER) || payload.startsWith(GET) || payload.startsWith(PUT) || payload.startsWith(DELETE) || payload.startsWith(POST)) {
            return HTTP;
        }
        if (buffer[0] == 0x16 && buffer[1] == 0x03) {
            return HTTPS;
        }
        if (bytesRead >= 5 && (buffer[4] == 0x10 || buffer[4] == 0x11 || buffer[4] == 0x12)) {
            return MYSQL;
        }

        if (payload.startsWith(SSH_HEADER)) {
            return SSH;
        }

        if(payload.startsWith(FTP_HEADER)){
            return FTP;
        }

        System.out.println(" 未知协议 ");
        return null;
    }
}

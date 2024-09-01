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

import com.luckylong.proxy.util.FileUtil;
import com.luckylong.proxy.util.ProtocolUtil;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 客户端请求类
 * @author xiaofeilong
 * @date 2024/8/3 15:42
 */
public class ClientProtocolForward implements Serializable {

    @Serial
    private static final long serialVersionUID = 1732603616413796217L;

    // 是否开启客户端协议转发
    private boolean enable;

    // 协议转发 目前仅支持 ssh  http 和https
    private Map<String, String> protocols;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public Map<String, String> getProtocols() {
        return protocols;
    }

    public void setProtocols(Map<String, String> protocols) {
        this.protocols = protocols;
    }

    public ClientProtocolForward() {
    }

    public ClientProtocolForward(boolean enable, Map<String, String> protocols) {
        this.enable = enable;
        this.protocols = protocols;
    }

    /**
     * 获取转发地址
     * @author xiaofeilong
     * @date 2024/8/3 17:49
     * @param [payload]
     * @return java.lang.String
     */
    public String getForward(byte[] buffer, int bytesRead) {
        if (!enable) {
            return FileUtil.getConfig().getDefaultForward();
        }
        String forward = protocols.get( ProtocolUtil.getProtocol(buffer, bytesRead));
        return forward != null ? forward : FileUtil.getConfig().getDefaultForward();
    }

    @Override
    public String toString() {
        return "ClientProtocolForward{" + "enable=" + enable + ", protocols=" + protocols + '}';
    }
}
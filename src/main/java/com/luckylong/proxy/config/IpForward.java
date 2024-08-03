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

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * ip请求转发
 * @author xiaofeilong
 * @date 2024/8/3 15:48
 */
public class IpForward implements Serializable {

    @Serial
    private static final long serialVersionUID = -7853924800473036212L;

    // 是否开启客户端请求ip转发
    private boolean enable;
    // key是转发的ip和端口     list是客户端请求的ip
    private Map<String, List<String>> forward;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public Map<String, List<String>> getForward() {
        return forward;
    }

    public void setForward(Map<String, List<String>> forward) {
        this.forward = forward;
    }

    public IpForward() {

    }

    public IpForward(boolean enable, Map<String, List<String>> forward) {
        this.enable = enable;
        this.forward = forward;
    }

    /**
     * 获取请求转发的ip
     * @param ip 请求ip
     * @return 转发ip
     */
    public String getForward(String ip) {
        if (!enable) {
            return null;
        }
        for (Map.Entry<String, List<String>> tempMap : forward.entrySet()) {
            if(tempMap.getValue().contains(ip)){
                return tempMap.getKey();
            }
        }
        return FileUtil.getConfig().getDefaultForward();
    }
}
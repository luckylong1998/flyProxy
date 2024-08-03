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

import com.luckylong.proxy.config.Config;
import org.yaml.snakeyaml.Yaml;

import java.io.*;

/**
 * 文件操作工具类
 * @author xiaofeilong
 * @date 2024/8/3 16:33
 */
public class FileUtil {

    private static final String CONFIG_FILE_NAME = "config.yaml";

    private static Config config = null;

    /**
     * 获取系统配置信息
     * @author xiaofeilong
     * @date 2024/8/3 16:48
     * @return com.luckylong.proxy.config.Config
     */
    public static Config getConfig() {
        if(config != null){
            return config;
        }
        InputStream inputStream = null;
        try {
            Yaml yaml = new Yaml();
            inputStream = getInputStream();
            // 工作目录找不到找jar包里面的
            config = yaml.loadAs(inputStream, Config.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            if(inputStream != null){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return config;
    }

    /**
     * 获取配置文件输入流
     * @author xiaofeilong
     * @date 2024/8/3 16:48
     * @throws FileNotFoundException
     */
    private static InputStream getInputStream() throws FileNotFoundException {
        // 工作目录找不到找jar包里面的
        String filePath = System.getProperty("user.dir") + File.separator + CONFIG_FILE_NAME;
        File file = new File(filePath);
        if (file.exists()) {
            return new FileInputStream(file);
        }
        return FileUtil.class.getClassLoader().getResourceAsStream(CONFIG_FILE_NAME);
    }
}

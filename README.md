## 介绍

本项目内容为Java 的tcp代理转发

- 可根据客户端请求ip进行转发请求
- 可根据客户端协议进行转发请求





## 配置说明

```yaml
#程序启动监听的端口号
port: 8080
#线程池数量
threadPoolSize: 100
#默认转发规则
defaultForward: "172.18.1.50:80"

#是否根据请求开启IP转发  ip转发和客户端协议转发互斥   当两个都开启的时候，以IP转发为准

#根据客户端请求ip转发
ipForward:
  enable: false
  forward:
    #如果客户端请求IP为  172.18.1.50  或者 172.18.1.18  则转发到172.18.1.50:3000
    "172.18.1.50:3000":
      - "172.18.1.50"
      - "172.18.1.12"
    "172.18.1.50:22":
      - "127.0.0.1"

#是否开启客户端协议转发   ip转发和客户端协议转发互斥
clientProtocolForward:
  enable: true
  #客户端协议转发规则
 #协议转发 客户端请求协议为  http 则转发到172.18.1.50:80  ssh 则转发到172.18.1.50:22
  protocols:
    http: "172.18.1.50:3000"
#    https: "www.baidu.com:443"
    ssh: "172.18.1.50:22"
#    mysql: "127.0.0.1:3306"
```



## 打包说明

> 先用Maven 打包成jar 再用 jpackage  打包成可执行的文件



```` shell
    jpackage    --input target   --name flyProxy  --main-jar fly-proxy.jar  --type app-image
````

上述命令中：

- `--input target`：指定包含生成的 JAR 文件的目录。
- `--name ProxyApp`：指定应用程序名称。
- `--main-jar proxy-1.0-SNAPSHOT-shaded.jar`：指定包含所有依赖项的 JAR 文件。
- `--main-class com.luckylong.proxy.Application`：指定主类（包含 `public static void main(String[] args)` 方法）。
- `--type app-image`：指定生成应用程序的类型（如 `app-image`、`exe`、`msi`、`pkg` 等）。
- `--module-path $JAVA_HOME/jmods`：指定模块路径。
- `--add-modules java.base,java.desktop`：指定应用程序所需的模块。
- `--runtime-image jre`：指定生成的运行时图像的名称。





**确定必要模块**：

- 通过分析你的应用程序确定哪些 Java 模块是必须的。通常，最小模块是 `java.base`，其他模块根据应用程序的需要添加。

**使用 `jdeps` 工具**：

- `jdeps` 是 JDK 提供的一个依赖分析工具，可以帮助你确定你的应用程序需要哪些模块。

- 运行以下命令来分析你的 `fly-proxy.jar` 所依赖的模块：

  ```shell
  jdeps --print-module-deps --ignore-missing-deps fly-proxy.jar
  ```

- 这个命令将输出你的应用程序所依赖的模块列表。例如，输出可能是：

  ```shell
  java.base,java.desktop,java.logging
  ```

**创建自定义 JDK**：

- 根据 jdeps 的输出使用 jlink

   创建一个只包含所需模块的自定义 JDK。例如：

  ```shell
  jlink --module-path $JAVA_HOME/jmods --add-modules java.base,java.desktop,java.logging --output flyProxy-jdk
  ```

- 最终打包命令

``` shell
    jpackage \
    --input target \
    --name flyProxy \
    --main-jar fly-proxy.jar \
    --type app-image \
    --runtime-image flyProxy-jdk
```


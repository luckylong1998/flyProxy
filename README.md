## 打包说明

```` shell
    jpackage    --input target   --name ProxyApp  --main-jar fly-proxy.jar  --type app-image
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


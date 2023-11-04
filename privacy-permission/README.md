# 隐私权限

## 设计思路

1. 整理隐私权限涉及的代码(类名及其属性名或方法名)
2. 基于[ApkAnalyzer](ApkAnalyzer.md)收集方法堆栈, 检测是否存在"隐私权限涉及的代码"
3. 使用POI生成Excel
4. 每个隐私权限涉及的代码调用堆栈存放在一个Excel中

## 使用

### 应用插件

Using the plugins DSL:
```groovy
plugins {
    id "io.github.jingtuo.privacy-permission" version "0.0.3"
}
```

Using legacy plugin application:
```groovy
buildscript {
  repositories {
    maven {
      url = "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "io.github.jingtuo:privacy-permission:0.0.3"
  }
}
```

### 配置

#### privacyPermission

```groovy
privacyPermission {
    //指定的目录必须存在
    cmdlineToolsDir = file("C:/Android/Sdk/cmdline-tools/latest")
    //指定的文件必须存在, 非加固包
    apkFile = file("D:/apk/test/test.apk")
    mappingFilePath = ""
    permissionSpecsFile = file("D:\\Projects\\github\\jingtuo\\Codespaces\\privacy-permission\\permissionSpecs.json")
    outputDir = file("D:\\apk\\test2")
}
```

#### permissionSpecsFile

```json
[
  {
    "name": "应用进程",
    "clsName": "android.app.ActivityManager",
    "isField": false,
    "methodName": "getRunningAppProcesses",
    "methodReturnType": "java.util.List"
  }
]
```

## 版本

### 0.0.3

1. 移除调试代码, 梳理几种常用三方库

### 0.0.2

1. 移除对Kotlin序列化插件的依赖, 使用Gson解析permissionSpecsFile文件

### 0.0.1

1. 基于Apk Analyzer收集隐私权限相关类的调用堆栈


## 问题

1. 已知与其他插件依赖的commons-io版本冲突, 不兼容依赖commons-io低版本的插件
2. 尽量使用最新版, 已在11.0版本中验证


## 参考资料

- [实现Gradle插件](https://docs.gradle.org/current/userguide/implementing_gradle_plugins.html)
- [懒加载属性](https://docs.gradle.org/current/userguide/lazy_configuration.html#lazy_properties)
- [ASM](https://asm.ow2.io/)
- [POI Excel](https://poi.apache.org/components/spreadsheet/index.html)

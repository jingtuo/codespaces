# 实验室

## 应用包构成

- META-INF/MANIFEST.MF, 记录各个文件的SHA-256-Digest, 格式如下:
    ```text
    Manifest-Version: 1.0
    Built-By: Signflinger
    Created-By: Android Gradle 7.4.1
  
    Name: AndroidManifest.xml
    SHA-256-Digest: begulktw1z49wZnRSYZaMGLwU4kTYq1zEuQJGON/42w=
  
    Name: META-INF/androidx.databinding_viewbinding.version
    SHA-256-Digest: HfHivnbc9lDwdu3LRhiw4UM/0AuLM8oqJpx6mhCj1tE=
    ```

> 当使用view-binding之后, 应用包会增加文件: META-INF/androidx.databinding_viewbinding.version, META-INF/MANIFEST.MF、META-INF/CERT.SF、classes.dex文件都会增大
> 即使不依赖kotlin进行开发, 打出来的应用包(apk)依然会包含kotlin目录

## Kotlin

### Coroutine

1. Coroutine类似线程, 但又跟线程不同
2. It may suspend its execution in one thread and resume in another one.

关键词:
1. launch负责定义Coroutine的工作内容, 同一作用域下的多个launch是并发运行
2. runBlocking负责定义Coroutine的作用域, 可能会阻断当前线程
3. coroutineScope负责定义Coroutine的作用域, 只可以被另一个作用域调用, 或者被suspend函数调用

## 安全(Security)

默认的Provider:
- AndroidNSSP: Android Network Security Policy Provider
- AndroidOpenSSL: Android's OpenSSL-backed security provider
- CertPathProvider: Provider of CertPathBuilder and CertPathVerifier
- AndroidKeyStoreBCWorkaround: Android KeyStore security provider to work around Bouncy Castle
- BC: BouncyCastle Security Provider v1.68
- HarmonyJSSE: Harmony JSSE Provider
- AndroidKeyStore: Android KeyStore security provider

> 默认的Provider并没有提供国密算法(SM2、SM3、SM4), org.bouncycastle:bcprov-jdk18on:1.72提供了国密算法, 但是BaseKeyGenerator.engineInit未实现


### 问题

1. 使用KeyGenerator.getInstance(algorithm, provider)生成SM4的密钥KEY, 遇到错误: java.security.InvalidAlgorithmParameterException: Not Implemented  
   暂时未明白: JceSecurity.getInstance("KeyGenerator", KeyGeneratorSpi.class, algorithm, provider)找到的是BaseKeyGenerator

## 定时任务

### 问题

| 类 | 属性/方法 | OPPO Reno 3 元气版 |
| :-- | :-- | :-- |
| AlarmManager | setInexactRepeating | 应用切后台锁屏, 闹钟无法触发, 解锁打开应用, 触发闹钟 |
| AlarmManager | setExactAndAllowWhileIdle | 应用切后台锁屏, 闹钟无法触发, 解锁打开应用, 触发闹钟 |
| AlarmManager | ACTION_NEXT_ALARM_CLOCK_CHANGED  与getNextAlarmClock配合使用, 下一个闹钟被修改(删除)之后触发 | 应用切换至后台, 不会收到广播 |
| PowerManager | isIgnoringBatteryOptimizations | false |
| Settings | ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS | No Activity found to handle Intent |
| Intent | ACTION_TIME_TICK  每分钟开始(05:00)触发一次 | 应用切后台锁屏, 闹钟无法触发, 解锁打开应用, 收到最近一次的广播 |
| Intent | ACTION_SCREEN_ON  | 应用处于前台, 点亮屏幕, 不论是否解锁成功, 应用就能收到广播; 应用处于后台, 不会收到广播 |
| RoleManager | createRequestRoleIntent(RoleManager.ROLE_DIALER) | 直接返回Activity.RESULT_CANCELED |


## 隐私权限

基于apkanalyzer检索apk中引用树的功能, 查找涉及隐私权限的代码调用堆栈, 详见[这里](privacy-permission/README.md)

### 版本

- 0.0.3
    - 支持扫描构造函数

## WebView

基于学习WebView的过程, 封装WebView组件, 详见[这里](webview/README.md)

## 课外知识

### 矢量图

1. group支持的属性动画, 支持的属性: rotation, translateX, translateY
2. 不支持帧动画

### 弧线函数

- M, 移动函数, 参数:
    - x,y 直接移动到某个点
- L和l, 连接函数, 参数:
    - x,y 连接到某个点, 直接多个坐标, 如:lx1,y1 x2,y2
- A和a, 参数:
    - 半径: xRadius,yRadius,
    - x轴旋转角度: xAxisRotation, 暂未搞明白如何对圆进行动画
    - 大弧小弧标志: largeArcFlag, 1-大弧, 如果画半圆大弧小弧无区别
    - 顺时针逆时针标志: sweepFlag, 1-顺时针
    - 终点: x,y

## 待学习

- [Publish Library](https://developer.android.google.cn/build/publish-library?hl=zh-cn)
- [Gradle Java ToolChains](https://docs.gradle.org/8.0/userguide/toolchains.html)

## 参考文献

- [Android Kotlin Start](https://developer.android.google.cn/kotlin/get-started)
- [Android Kotlin Learn](https://developer.android.google.cn/kotlin/learn)
- [Android KTX](https://developer.android.google.cn/kotlin/ktx)
- [Shrink Code](https://developer.android.google.cn/studio/build/shrink-code.html?hl=en)



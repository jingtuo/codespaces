# Codespaces

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

## kotlin

1. 即使不依赖kotlin进行开发, 打出来的应用包(apk)依然会包含kotlin目录


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




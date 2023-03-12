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
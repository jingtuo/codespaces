# 生物识别

## 版本兼容

- Android 11(API 30)才支持拉起"生物识别注册页面"
- Android 6(API 23) KeyProperties.KEY_ALGORITHM_AES、KeyProperties.PURPOSE_ENCRYPT、 KeyProperties.PURPOSE_DECRYPT

## 实践

### 场景1

机型: 小米Pad 5 Pro(Android 12)  

BiometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL)返回值为BiometricManager.BIOMETRIC_SUCCESS
BiometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)返回值为BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE, 但是设备明明有"人脸解锁"功能
BiometricPrompt.PromptInfo.setAllowedAuthenticators=true, 不能调用setNegativeButtonText, 否则调用build时报如下错误
> Negative text must not be set if device credential authentication is allowed.

机型: Oppo Reno3 元气版  

BiometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)返回值为BiometricManager.BIOMETRIC_SUCCESS, 但是调用BiometricPrompt.authenticate仅拉起指纹识别  

机型: vivo S10 Pro(Android 11)  

不支持KeyGenParameterSpec.Builder.setIsStrongBoxBacked(true)



### 算法

以下算法已经验证: 
- AES + CBC + PKCS7Padding
 
## 相关知识

- Security.getProviders: 
  - AndroidNSSP
  - AndroidOpenSSL
  - CertPathProvider
  - AndroidKeyStoreBCWorkaround
  - BC
  - HarmonyJSSE
  - AndroidKeyStore

### 安全

- AndroidKeyStore 
  - 进程隔离, 加密解密在系统进程中执行, 脱离应用进程

## 意外知识

- android.util.Patterns提供邮箱正则表达式Patterns.EMAIL_ADDRESS

## 参考文献

- [Biometric Guide](https://developer.android.google.cn/training/sign-in/biometric-auth?hl=zh-cn)
- [Biometric Release](https://developer.android.google.cn/jetpack/androidx/releases/biometric?hl=zh-cn)
- [Login with Biometrics](https://developer.android.google.cn/codelabs/biometric-login?hl=en#0)
- [Data Security](https://developer.android.google.cn/topic/security/data?hl=zh-cn)
- [Security Release](https://developer.android.google.cn/jetpack/androidx/releases/security?hl=en)
  - 1.0.0 仅支持Android 6.0(API 23)以上的设备
- [Android 密钥库系统](https://developer.android.google.cn/training/articles/keystore?hl=zh-cn)
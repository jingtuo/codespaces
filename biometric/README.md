# 生物识别

## 版本兼容

- Android 11(API 30)才支持拉起"生物识别注册页面"
- Android 6(API 23) KeyProperties.KEY_ALGORITHM_AES、KeyProperties.PURPOSE_ENCRYPT、 KeyProperties.PURPOSE_DECRYPT

## 兼容问题


### 场景1

机型: 小米Pad 5 Pro(Android 12)

BiometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL)返回值为BiometricManager.BIOMETRIC_SUCCESS  

BiometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)返回值为BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE, 但是设备明明有"人脸解锁"功能  

BiometricPrompt.PromptInfo.setAllowedAuthenticators=true, 不能调用setNegativeButtonText, 否则会调用build时报如下错误
> Negative text must not be set if device credential authentication is allowed.
  
## 相关知识

- Security.getProviders: 
  - AndroidNSSP
  - AndroidOpenSSL
  - CertPathProvider
  - AndroidKeyStoreBCWorkaround
  - BC
  - HarmonyJSSE
  - AndroidKeyStore

## 意外知识

- android.util.Patterns提供邮箱正则表达式Patterns.EMAIL_ADDRESS

## 参考文献

- [用户指南](https://developer.android.google.cn/training/sign-in/biometric-auth?hl=zh-cn)
- [版本](https://developer.android.google.cn/jetpack/androidx/releases/biometric?hl=zh-cn)
# WebView

学习Android WebView组件, 并封装成组件

## Android Browser Helper

### 验证

1. 荣耀X40(Android 12)是打开一个单独的页面, 该设备其支持CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION
2. 小米5 Pad Pro(Android 13)是直接在浏览器中, 该设备不支持CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION
3. vivo S10 Pro(Android 13)是直接在浏览器中, 该设备不支持CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION


## 课外知识

1. 百度的scheme
   - 语音搜索：baiduboxapp://speech/startVoiceSearch
   - 图片搜索：baiduboxapp://browser/imageSearch

## 参考资料

- [Web Apps](https://developer.android.google.cn/guide/webapps?hl=zh-cn)
- [Android Browser Helper](https://github.com/GoogleChrome/android-browser-helper)
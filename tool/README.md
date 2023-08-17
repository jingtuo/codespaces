# 工具

## Android应用启动时长统计

### 思路

1. 基于adb logcat收集日志
    ```shell
    # Window
    adb logcat -T 1 -v year,threadtime *:S ActivityTaskManager WindowManager | Out-File -Encoding utf8 20230817-1.log
    ```
2. 分析日志
   1. Activity.onResume到WindowManager的'Changing Focus null to Window'有一定的时间
   2. WindowManager的'Changing Focus null to Window'到ActivityTaskManager的'Displayed'有一定的时间
   3. 基于ActivityTaskManager的'START'和'Displayed'分析数据
3. 将日志导出启动时长数据

### 实践

1. 荣耀X40 有TAG: ActivityTaskManager WindowManager ActivityManager
2. vivo X30 只有TAG: ActivityTaskManager, 并且没有Start日志, 只有Displayed日志
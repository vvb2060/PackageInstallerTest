# PackageInstaller 静默升级 演示应用

用于检测 [PackageInstaller API](https://developer.android.com/reference/android/content/pm/PackageInstaller) 是否正常的测试应用。
该 API 自 API 级别 21（Android 5.0）起可用。
从 API 级别 31（Android 12）起，PackageInstaller 支持静默升级。

## MIUI 破坏了此 API

在MIUI 12.5 之前，开启 MIUI 优化时无法正常使用。具体表现为`com.miui.packageinstaller`未实现`android.content.pm.action.CONFIRM_INSTALL`。
```
android.content.ActivityNotFoundException: No Activity found to handle Intent { act=android.content.pm.action.CONFIRM_INSTALL flg=0x10000000 pkg=com.miui.packageinstaller (has extras) }
```
关闭 MIUI 优化时使用 Google 安装器，恢复正常。


在 MIUI 12.5 及以后，部分版本恢复正常，随即又被破坏。问题来源于系统，无法通过升级软件包安装器修复，但依然可通过关闭 MIUI 优化绕过问题逻辑。
```
E/PKMSImpl: MIUILOG- assertCallerAndPackage: uid=10018, installerPkg=io.github.vvb2060.packageinstaller.test, msg=Permission denied
```

## API 注意事项

在 Android 12 之前，此 API 常见用途为安装拆分包，非拆分 apk 大多使用 intent 传递给系统安装器安装。

在 Android 12 及以后，由于支持静默升级，可能会全面开始使用此 API，这是本应用诞生的直接原因：提供示例代码以供测试。

静默升级的要求有：
1. 已经允许安装未知应用操作。可主动要求用户开启，也可发起一次安装，系统自会要求用户开启。
2. 升级自身或由自己安装的应用。升级其它应用时，需要该应用的安装者为自己，即以前通过 PackageInstaller API 安装的应用，系统安装器无效。
3. apk 已经适配 Android 10 或更高。即 Target API >= 29，此条件以后会增加。
4. 30 秒以内没有静默升级过同一款应用。参考 [SilentUpdatePolicy](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-12.0.0_r26/services/core/java/com/android/server/pm/SilentUpdatePolicy.java)。
5. 永远做好接收`STATUS_PENDING_USER_ACTION`的准备。

用户非常容易把静默升级当成应用崩溃，因此需要合理的引导。
应用打开时触发的升级可接收`android.intent.action.MY_PACKAGE_REPLACED`广播弹出更新完成通知。
但更建议在后台进行升级，如果需要用户确认可发送通知。

由于安装会话除非显式放弃，否则一直可用，包括在重启设备后，因此也可等待下次打开应用时让用户确认（此功能本演示应用未实现，一律直接丢弃）。

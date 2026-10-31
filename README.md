# SamsungCTS

[English](README_en.md)

面向三星 One UI 的 Google 圈定搜索工具，基于 [parallelcc/MiCTS](https://github.com/parallelcc/MiCTS) 修改，采用 GPL-3.0。

无需 root。通过 Shizuku 或电脑 ADB 一次性授权，配置底角助理手势，并在开机解锁后恢复。搜索功能由 Google 应用提供，SamsungCTS 本身不联网、不读取屏幕、没有常驻服务。

## 功能

- 从屏幕左下角或右下角向内斜上滑，打开 Google 圈定搜索。
- 应用内测试入口、快捷面板按钮、桌面图标长按快捷方式。
- Google、设置授权、后台自动运行和当前配置状态检查。
- 开机解锁后恢复设置，并进行一次有限复核。
- 保存助理与导航设置原值，可恢复原配置；区分未设置与空字符串。
- 简体中文、英文和跟随系统的深浅主题。

此分支移除了上游的 Xposed/LSPosed 模块、hook、模块元数据、机型伪装及相关依赖，也不使用 `org.lsposed.hiddenapibypass`。调用通过系统 Binder 完成，不需要隐藏 API 豁免或修改系统文件。

## 配置

1. 安装并更新 Google 应用，将 Google 设为默认数字助理。首次出现三星助理选择窗口时，选择 Google 并确认。
2. 安装 SamsungCTS，选择一种一次性授权方式：

   - **Shizuku**：通过无线调试或 ADB 启动 Shizuku 13+，回到 SamsungCTS 点击“使用 Shizuku 授权”，允许请求。应用只为自己授予设置权限，完成后关闭临时授权服务。之后不需要 Shizuku 保持运行。
   - **电脑 ADB**：开启 USB 调试，在电脑执行：

   ```sh
   adb shell pm grant dev.valohalo.samsungcts android.permission.WRITE_SECURE_SETTINGS
   ```

3. 在 SamsungCTS 的应用程序信息 → 电池/后台运行中选择 **不受限制**，并确认允许。国行可能默认禁止新应用自启动，导致开机广播被跳过。
4. 在应用中点击 **启用底角手势**。需使用系统滑动手势导航。
5. 用底角斜滑或应用内测试入口验证搜索。如果首次助理选择覆盖了配置，选择 Google 后回应用重新启用。

如果此前使用独立的“Google 圈搜手势”辅助应用，先在旧工具中恢复原配置，再启用 SamsungCTS，避免开机时相互覆盖。SamsungCTS 不需要安装原版 MiCTS。

导航条正中长按保留系统原有行为。更改默认数字助理、相关导航设置或更新系统后，可能需要重新启用并验证。卸载前使用 **恢复原配置**。

## 兼容范围

- 最低 Android 13，界面配置面向三星 One UI。
- Binder 格式已核对 AOSP Android 13–17，以及 S23 国行 Android 16 和 S24 港行 Android 17 的框架文件；实际功能仍取决于设备固件和 Google 应用支持。
- 首个实机验证目标为 S23 CHC / One UI 8.5。测试状态见 [验证记录](docs/VALIDATION.md)。
- Google 搜索需要可用网络。本应用不提供代理，不包含 GMS 策略调整，也不能保证其他应用如何判定设备环境。

## 权限

主动申请 `WRITE_SECURE_SETTINGS`（Shizuku 或 ADB 一次授予）、`RECEIVE_BOOT_COMPLETED`，以及可选 Shizuku 客户端授权。没有网络、无障碍、悬浮窗、截图或 root 权限。仅修改三个已备份的系统设置：`assistant`、`voice_interaction_service`、`navigationbar_splugin_flags`。

Shizuku 接入只接受 ADB 身份，通过受跨用户权限保护的 provider 接收 Binder；不初始化 Sui。授权服务没有通用命令执行接口，只能为本应用授予 `WRITE_SECURE_SETTINGS`，并在完成或超时后销毁。Debug 与 Release 均启用裁剪，并强制检查未使用的 Sui 类已移除。正常的 Shizuku 客户端声明会保留，不承诺其他软件无法识别。

## 构建

使用 JDK 17、Android SDK 36，以及仓库自带的 Gradle Wrapper：

```sh
./gradlew :app:assembleDebug :app:lintDebug
```

调试 APK 位于 `app/build/outputs/apk/debug/`。正式分发时使用自己的稳定签名密钥；不要提交密钥或密码。CI 构建调试 APK，不自动发布版本。

## 来源与许可证

本项目继承 MiCTS 的圈定搜索调用思路、快捷磁贴与部分界面资源，三星配置与开机恢复由本 fork 增加。保留上游 Git 历史及 [GPL-3.0 许可证](LICENSE)。Shizuku API 与 Binder 传输协议来自 RikkaW，保留其 [MIT 许可证](app/src/main/assets/SHIZUKU-LICENSE.txt)。修改版发布时须提供对应源码和许可证，不应将修改版问题归于上游作者。

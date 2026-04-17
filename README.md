# VPPassword (🔐 Version 1.2.0)

**[中文说明](#✨-核心特性) | [English Summary](#🛡️-security-architecture)**

VPPassword 是一款基于 **Jetpack Compose** 打造的高度安全、极美视觉的 Android 个人密码管家。它不仅提供军用级的加密保护，还拥有独特的“老钱风”(Quiet Luxury) 审美设计，让您的数据管理既安全又优雅。

---

## ✨ 核心特性

- 🛡️ **极简三位一体安全架构**：
    - **SQLCipher**：全数据库级别加密，确保存储文件即使离开手机也无法被破解。
    - **Android Keystore**：硬件级密钥存储，主密钥永远不会离开系统的安全岛。
    - **指纹/生物识别**：极速解锁，确保唯有您可以访问敏感信息。
- 🎨 **“老钱风”审美体系**：
    - 预设 8 套精心调优的主题，包括：*象牙丝绸 (Ivory Silk)*、*皇家海军 (Royal Navy)*、*牛津红酒 (Oxford Wine)* 等。
    - 告别刺眼的霓虹色，拥抱低调、内敛的奢华质感。
- 🖼️ **深度背景定制系统**：
    - 支持自定义背景图。
    - **动态高斯模糊**：支持 0-20dp 实时调节背景模糊度，确保无论背景多复杂，UI 始终清晰可读。
    - **亮度调节 (Scrim)**：灵活的遮罩亮度滑块，让背景与内容的层次感达到完美。
- ⚡ **极致性能与交互**：
    - **满血刷新率**：完美适配 120Hz/144Hz 屏幕，滑动如丝绸般顺滑。
    - **批量管理**：支持长按多选、一键删除，轻松打理海量记录。
    - **智能排序**：支持按项目名称、创建时间进行多种维度的快捷排序。
- 📦 **灵活导入导出**：
    - 支持加密备份导出。
    - 支持明文 CSV/JSON 迁移。

---

## 🛡️ Security Architecture

VPPassword follows the "Zero Trust" principle for local data:
- **AES-256 Encryption**: Every byte in the Room database is secured via SQLCipher.
- **Hardware-Backed Keys**: On supported devices, encryption keys are generated and stored in the Android hardware security module (TEE/StrongBox).
- **Zero Network Traffic**: This app does NOT request INTERNET permission. Your passwords never leave your device.

---

## 🛠️ 技术栈 (Tech Stack)

- **UI**: Jetpack Compose (Material 3)
- **Database**: Room Persistence Library + SQLCipher
- **Architecture**: MVVM + Flow + Coroutines
- **Security**: BiometricPrompt + Android Keystore System
- **Serialization**: Gson

---

## 🚀 如何安装

1. 克隆代码仓库：`git clone https://github.com/zhen731/VPPassword.git`
2. 使用 Android Studio (Hedgehog 或更高版本) 打开。
3. 同步 Gradle 并运行。

---

## ⚖️ 许可证

本项目基于 MIT License 开源。

---

**🔐 VPPassword - 让您的数字生活更优雅、更安全。**

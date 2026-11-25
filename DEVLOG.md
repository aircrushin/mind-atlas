# DEVLOG · 空间研读台 MindAtlas（PICO Space Program 2026 · Spatial 空间应用赛道）

团队：黑客松大哥（1 人：傅玉涵） · 报名凭证 PICO-62CC726D28 · 截止 2026-12-19 09:00（北京时间）

## AI 工具使用记录（提交表“创作过程”栏需要）
| 项 | 内容 |
|---|---|
| AI IDE / Agent | Grok Bot（自主编码 Agent，运行在 Linux 云主机上，命令行 + Gradle 构建） |
| 主要模型 | Grok Bot 内置模型（编码）；应用内 AI：OpenRouter → `deepseek/deepseek-chat-v3-0324`（可在设置中改） |
| PICO CLI | `@picoxr/pico-cli`（`emulator doctor` / `emulator install` 用于环境诊断，记录见下） |
| 说明 | 代码由 Agent 根据 PICO Spatial SDK 6.2.8 源码与官方 Samples（SpatialUI、WelcomeSpace）编写；人负责选题、评审与真机验证 |

## 日志

### 2026-11-25（北京时间）
- 读规则 PDF：提交要 GitHub 仓库 + Commit ID、可安装 APK、操作说明、图标/海报/详情图（JPG，规格见 SUBMISSION.md）、AI IDE 与模型说明。
- 调研 SDK：官方 Samples 用 AGP 8.8 / Kotlin 2.0 / compileSdk 35 / minSdk 35，`com.pico.spatial:bom:6.2.8`，依赖来自公开 Maven `artifact.bytedance.com/repository/Volcengine`（含 sources.jar，便于查 API）。
- 在 Linux 主机上搭好工具链：Temurin JDK 17、Android cmdline-tools、platform 35、build-tools 35.0.0、Gradle 8.10.2 wrapper。官方 SpatialUI 示例 `assembleDebug` 一次通过。
- 模拟器：`pico-cli emulator doctor` 报告 `Platform: unsupported`（PICO Emulator 只发布 Windows 与 Apple Silicon macOS 包），本机无法运行；改为编译期验证 + JVM 单元测试，真机/模拟器验证需在 Windows/Mac 或线下实机调试点完成。
- 实现 MVP：6 种空间窗口（阅读器主窗、笔记、AI 助手、知识星图体积窗口、单卡片多实例窗口、设置），共享 StateFlow 状态，JSON 原子持久化；OpenRouter AI 提炼卡片 / 自动关联 / 问答，断网自动降级到本地 bigram 整理。首次构建通过。
- 在本机尝试用 Google 官方 Android Emulator（API 35 x86_64）做冒烟测试：KVM 为嵌套虚拟化，qemu 创建 VM 后卡在启动前（无 vCPU 线程、无内核输出），放弃；PICO Emulator 官方也不支持 Linux。
- AI 提示词用 OpenRouter 实测：卡片提炼 ~5s、关联 ~5s，返回 JSON 结构正确。
- 加入 JVM 单元测试 5 个（全部通过）；release APK 构建通过，确认 APK 内不含任何 API Key。
- 新增 `proxy/worker.js`：可选的服务端代理，Key 不进 APK。

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
### 2026-11-26（北京时间）
- 在用户 Windows 电脑（DESKTOP-7RD0D2K）上把工具链装到 `D:\pico-spatial`：`@picoxr/pico-cli@0.7.0`（npm 前缀在 D:，约 29 分钟），`PICO_HOME=D:\pico-spatial\pico-home`，`C:\Users\Admin\.pico` 与 `picoAvdConfig` 做目录联接到 D:。
- 官方 PICO Emulator **6.2.1**（PICO OS 6 / swan / `picosdk_spaceos_x86_64-user`）安装并启动 AVD `Pico_Emulator_6_2`，adb `emulator-5554`。未使用自带的 Google Android Emulator。未装 Android Studio，未跑 `pico-cli setup --agent-tool`。
- 安装并启动 `MindAtlas-0.1.0-nokey.apk`（`com.aircrushin.mindatlas/.LaunchActivity`）。用 `pico-cli interact` 点开笔记、AI 助手、知识星图、设置，并用离线整理生成 1 张「遗忘·卡片」，六种空间窗口都有 2560×1440 截图，见 `/workspace/pico-spatial/screenshots/`（推荐投稿图 `05-spatial-2560x1440.png`）。
- 稳定性：2026-11-26 01:13:18–01:43:18（Asia/Shanghai）应用进程一直是 pid 5589，模拟器进程 69132 未退出，crash buffer 为空。详细命令与问题见 `/workspace/pico-spatial/windows-test-report.md`。

- Phase 2（同日稍后）：修正未配置 API Key 时状态栏仍显示「AI 在线 · deepseek/…」的问题。`AiConfig.usable` 现在只有在未开「仅离线」、Base URL 非空，并且（已填 Key，或 Base URL 指向非 OpenRouter 的代理）时为真；默认 OpenRouter 且 Key 为空时状态栏显示「AI 未配置（本地整理）」。空知识星图不再是几乎透明的方洞，增加体积背板、前景边框、可随视角旋转的占位星点，以及高对比说明卡片；有卡片后的星图绘制路径未改。
- 投稿素材写入 `submission-assets/`：图标 1024×1024 JPG、海报 2560×1440 / 1024×1024 JPG、详情图 2560×1440 JPG（由官方模拟器推荐截图转码）。图标与海报基于已有星图标识和应用名「空间研读台 MindAtlas」，未写入密钥或个人隐私。
- 版本升至 0.1.1（versionCode 2），在无 `AI_API_KEY` / `LLM_API_KEY` / `secrets.properties` 的环境中重建 release APK：`/workspace/pico-spatial/apk-releases/MindAtlas-0.1.1-nokey.apk`。已扫描包内无 Key 字符串。JVM 单元测试 7 个全部通过（含 `usable` 用例）。
- GitHub 仓库已公开：https://github.com/aircrushin/mind-atlas 。本轮未在官方模拟器上复测 0.1.1 的两处 UI 修改，也未向官方参赛网站提交作品。

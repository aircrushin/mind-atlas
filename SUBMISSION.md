# 提交清单（PICO Space Program 2026 · 截止 2026-12-19 09:00 北京时间）

本轮只准备材料和公开仓库，**尚未在官方参赛网站提交作品**。

| 材料 | 状态 | 备注 |
|---|---|---|
| 作品名称 / 团队 / 联系人 / 赛道 / 技术栈 / 内容分类 | 草稿 | 空间研读台 MindAtlas；黑客松大哥；傅玉涵；Spatial 空间应用创作；Kotlin + PICO Spatial SDK 6.2.8 + Compose；学习/效率 |
| 可安装 APK | ✅ 已构建 0.1.1（无密钥） | `/workspace/pico-spatial/apk-releases/MindAtlas-0.1.1-nokey.apk`（包名 `com.aircrushin.mindatlas`，入口 `.LaunchActivity`，versionCode 2）。依赖：联网可选（AI），无需账号；测试条件：PICO OS 6。已在官方 PICO Emulator 6.2.1 上安装运行；0.1.1 的两处 UI 修复本轮未再上模拟器复测 |
| 操作说明 | ✅ README.md | |
| GitHub 仓库 + Commit ID + 版本名 + 版本说明 | ✅ 已公开 | 仓库 https://github.com/aircrushin/mind-atlas（公开）；版本 **0.1.1**；Commit ID 以 `origin/main` 最新提交为准。版本说明：修复未配置 Key 时误显示「AI 在线」、空知识星图可见占位，并补齐投稿图标/海报/详情图 |
| 一句话简介 / 应用简介 / 内容标签 / 特性标签 | 草稿见下 | |
| 图标 1024×1024 JPG ≤4MB | ✅ | `submission-assets/icon-1024.jpg`（1024×1024，约 69 KB） |
| 海报 2560×1440 JPG + 1024×1024 JPG ≤5MB | ✅ | `submission-assets/poster-2560x1440.jpg`（约 674 KB）、`submission-assets/poster-1024.jpg`（约 195 KB） |
| 详情页图 ≥1 张 2560×1440 JPG/PNG ≤5MB | ✅ | `submission-assets/detail-spatial-2560x1440.jpg`（2560×1440，约 726 KB；来自官方模拟器截图 `05-spatial-2560x1440.png`） |
| 宣传视频（选填） | ⏳ 未做 | 选填，本轮未制作 |
| AI IDE / 主要模型 | ✅ 见 DEVLOG.md | AI IDE：Grok Bot |
| PICO CLI 使用记录 | ✅ 见 DEVLOG.md | 官方模拟器 6.2.1 已在用户 Windows 上安装并完成 30 分钟稳定性测试 |

准入门槛自查：安装/启动/退出 · 核心功能真实可用 · 连续 30 分钟无崩溃 · 无阻断问题 · 无隐私风险（Key 不入库、数据仅存本机） · 材料齐全。前三项已在官方 PICO Emulator（PICO OS 6）上用 0.1.0 实测通过；0.1.1 为同一代码路径上的 UI/文案修复与投稿素材，本轮未再上模拟器。

一句话简介：空间桌面上的 AI 工作台，让阅读、笔记和知识图谱在你身边同时展开。
内容标签：学习、效率、AI、知识管理　特性标签：多窗口、体积窗口、AI 助手、离线可用

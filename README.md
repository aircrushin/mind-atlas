# 空间研读台 MindAtlas

> 空间桌面上的 AI 工作台：多窗口并排阅读、笔记与知识卡片，AI 助手自动整理、关联并用空间布局呈现知识图谱，让学习与研究更沉浸高效。

PICO OS 6 原生空间应用（PICO Spatial SDK 6.2.8 · Kotlin · Jetpack Compose / Spatial UI）。
参赛：PICO Space Program 2026 · Spatial 空间应用创作赛道 · 团队「黑客松大哥」。
公开仓库：[aircrushin/mind-atlas](https://github.com/aircrushin/mind-atlas) · 当前版本 **0.1.1**。

已在官方 PICO Emulator 6.2.1（PICO OS 6）上通过安装、六种空间窗口、2560×1440 截图，以及 30 分钟无崩溃稳定性测试（2026-11-26 01:13:18–01:43:18，北京时间）。未配置 API Key 时自动走本地整理。

## 空间窗口
| 窗口 | 形态 | 作用 |
|---|---|---|
| 阅读器（主窗口） | Planar，可缩放 | 书架 + 正文；点选段落 → 生成知识卡片 / 摘录到笔记；底部 Toolbar 召唤其他窗口 |
| 笔记 | Planar | 随手记；任一笔记一键转成卡片 |
| AI 助手 | Planar | 结合当前文档与全部卡片问答；快捷指令（总结、自测题、找联系、类比） |
| 知识星图 | **Volumetric**（1100×800×600dp） | 卡片作为节点悬浮在不同深度（`Modifier.zOffset`），关联为 3D 点线；拖拽旋转、自动环绕；点选节点高亮邻居。空图有可见的体积边框、占位星点和说明，避免看起来像透明方洞 |
| 卡片窗口 | Planar，**多实例**（按卡片 tag） | 把任意卡片“拿出来”并排比较，沿关联继续打开下一张 |
| 设置 | Planar | AI 接口地址 / Key / 模型，测试连接，离线模式，重置示例数据 |

所有窗口共享一个进程内 `StateFlow`（`data/Workspace.kt`），任一窗口的修改实时同步到其他窗口；状态原子写入 `filesDir/workspace.json`，重启不丢数据。

## AI
- 任意 OpenAI 兼容接口，默认 OpenRouter + `deepseek/deepseek-chat-v3-0324`。
- 卡片提炼、自动关联使用 JSON 输出；问答带当前文档与卡片上下文。
- **断网/未配置时自动降级**到本地整理（中文 bigram 关键词 + Jaccard 相似度关联、本地检索问答），核心流程始终可用。
- 状态栏仅在确实能发起在线请求时显示「AI 在线」（已填写 API Key，或 Base URL 指向自建/第三方代理）。默认 OpenRouter 地址且 Key 为空时显示「AI 未配置（本地整理）」，而不会误显示在线。

### Key 不进仓库
`secrets.properties`（已在 .gitignore）或同名环境变量在构建时注入默认值：
```
AI_BASE_URL=https://openrouter.ai/api/v1
AI_API_KEY=sk-or-...
AI_MODEL=deepseek/deepseek-chat-v3-0324
```
不提供时 APK 内不含任何 Key，用户在“设置”窗口填写（仅保存在本机 SharedPreferences）。
更安全的做法：部署 `proxy/worker.js`（Cloudflare Worker，Key 只在服务端），App 的 Base URL 指向代理。

## 构建
需要 JDK 17、Android SDK（platform 35 / build-tools 35）。
```
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease      # 用 debug 签名，便于评审直接安装；当前版本 0.1.1
./gradlew testDebugUnitTest    # 离线大脑、3D 布局、持久化、AI 可用状态单元测试
```
不要把 `AI_API_KEY` / `LLM_API_KEY` 或 `secrets.properties` 带进构建环境；不提供时 APK 内不含任何 Key。
无密钥 release 副本：`/workspace/pico-spatial/apk-releases/MindAtlas-0.1.1-nokey.apk`（包名 `com.aircrushin.mindatlas`，入口 `.LaunchActivity`）。
安装运行：`pico-cli app install <apk>` 或 `adb install -r <apk>`，在 PICO OS 6 设备 / PICO Emulator 上启动“空间研读台”。

投稿素材（图标、海报、详情图）在 [`submission-assets/`](submission-assets/)，清单见 [`SUBMISSION.md`](SUBMISSION.md)。

## 操作说明
1. 打开应用，主窗口左侧书架有 3 篇内置文章，也可“＋ 导入文本”粘贴自己的材料。
2. 点选一段或多段 → “✦ 生成知识卡片”；不选则全文生成。知识星图窗口自动弹出，新卡片出现并与已有卡片连线。
3. 在星图里拖拽旋转、点节点看摘要与关系，“在新窗口打开”把卡片拿到身边；卡片窗口里点关联卡片继续展开。
4. 打开“AI 助手”提问或用快捷指令；打开“笔记”记录想法并转为卡片。
5. “设置”里可切换 AI 服务或仅离线模式。

## 许可
代码 Apache-2.0（与官方 Samples 一致）。内置示例文章为原创文本。

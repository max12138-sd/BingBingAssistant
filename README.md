# 冰冰助手（BingBing Assistant）

小冰冰传奇怀旧服离线助手 —— 常驻悬浮球：竞技场阵容推荐 / 巅峰 3队·5队 方案生成 / 7 天活动提醒 / 我的英雄库 / 战果回填校准。

**唯一需求依据**：`小冰冰传奇怀旧服助手-需求与实现规格书-v1.1.md`（放本仓库 `docs/` 下）。本文档只描述工程结构与维护方式，功能细节以规格书为准。

## 快速开始

### 环境要求
- JDK 17+（本机已装：`D:\Environment\JDK`，Java 21）
- Android SDK（本机已装：`C:\Users\24346\AppData\Local\Android\Sdk`，platform 34 / build-tools 34.0.0）
- Gradle 8.7（wrapper 会自动下载；本机已有独立安装 `C:\Users\24346\tools\gradle-8.7`）

### 命令行构建
```bash
# Windows（Git Bash）
cd D:/Project/BingBingAssistant
./gradlew assembleDebug          # 产出 app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # JVM 单元测试（算法/导入器）
```

### Android Studio
直接 Open 本目录即可（`local.properties` 已指向本机 SDK；换机器删掉重建会自动生成）。

## 当前里程碑状态（§11）

| 里程碑 | 内容 | 状态 |
| --- | --- | --- |
| M0 工程骨架 | Gradle 可编译、OpenCV/ONNX 依赖、Room 全实体、SDK 日志 | ✅ **编译验证通过**（assembleDebug 产出 APK） |
| M1 我的英雄库 | 种子导入(20 示例英雄)、网格勾选、星级、体检、CSV 导入 | ✅ 代码完成，待真机验收 |
| M2 悬浮窗+截图 | 浮球(拖动/吸附/降透明)、面板骨架、MediaProjection 服务、权限引导 | ✅ 代码完成，待真机调参 |
| M3 竞技场推荐 | ScreenClassifier/SlotDetector/HeroMatcher/OcrEngine | 🔶 接口与降级就绪，识别算法 TODO(M3) |
| M4 巅峰3队 | 算法链路完整实现（§5.2~5.4），识别侧 TODO | 🔶 算法可用（设置页有自测按钮），UI Tab 待接 |
| M5 活动日历 | CalendarGridParser TODO(M5)；分组/提醒/手动导入已可用 | 🔶 |
| M6 巅峰5队+校准 | generate5(模拟退火)/Calibrator 已实现；真机性能调优待做 | 🔶 |

**验收命令**：`./gradlew assembleDebug testDebugUnitTest` —— 2026-11-23 首次验证：**36 个测试全过、0 失败**（含 §12 关键用例的 JVM 部分：T1/T2 降级、T3 采样约束、T9 方案去重）。
debug APK ≈ 67MB（双 ABI、未混淆）；release 开 R8+资源收缩，预计回到 §10 的 60MB 预算内，打 release 包后复核。

## 工程结构（对照规格书 §3）

```
app/src/main/java/com/bingbing/assistant/
├── App.kt                  # Application：Logger/DB/OpenCV/种子/每日提醒
├── core/                   # 手写单例(AppGraph) + 权限/ROM/设备/日志/事件总线
├── capture/                # ScreenCaptureService(前台+投影) / FrameGrabber
├── overlay/                # OverlayService / FloatingBallView / PanelController
├── vision/                 # LayoutProfile(已实现) + 识别器骨架(TODO M3/M5)
├── data/
│   ├── db/                 # AppDatabase + 4 DAO
│   ├── entity/             # 8 实体（字段照抄 §4.1）
│   ├── repo/               # 4 仓储
│   └── import/             # AssetSeeder / JsonImporter / CsvImporter / HeroLibDoctor
├── algo/                   # §5 全部算法（纯 Kotlin，可 JVM 单测）
├── feature/                # arena / peak / activity / pool 流程编排
└── ui/                     # 4 个 Activity（传统 View + ViewBinding）
app/src/main/assets/
├── seed/                   # heroes(20)/activities/templates/counter_rules/usage_priors
├── layouts/                # 归一化坐标布局（占位值，真机校准后替换）
├── models/ocr/             # TODO(M3)：放入 det/rec/cls.onnx + keys.txt
└── heroes/                 # TODO(M3)：heroId.webp 头像模板
```

## 与规格书的偏差说明（已登记，不违反铁律）

| # | 偏差 | 原因 |
| --- | --- | --- |
| 1 | Manifest 多了 `FOREGROUND_SERVICE_MEDIA_PROJECTION` / `FOREGROUND_SERVICE_SPECIAL_USE` 两条权限，且服务声明了 foregroundServiceType | targetSdk 34 起系统强制，不加直接崩溃；非新增能力，铁律 L1-L5 不受影响 |
| 2 | `LineupTemplateEntity` / `CounterRuleEntity` 字段为约定最小集（规格书未定义） | 代码内已标 `TODO(SPEC-MISSING)`，真实数据结构确定后在实体 + `seed/*.json` 两处同步改 |
| 3 | 星级成长公式（SimConfig.STAR_*_GROWTH）为线性近似 | 规格书未给数值，全部集中在 SimConfig，便于校准 |
| 4 | 隐藏位采样"全局不重复"按 §5.4.1 原文实现 | 若实测游戏允许跨队重复，改 `OpponentSampler` 一处即可 |
| 5 | 种子英雄为示例数据（20 名），非真实游戏数值 | M1 验收要求先用示例跑通；真实数值走 CSV/JSON 导入 |

## 如何新增/变更需求（维护空间预留）

1. **登记**：在 `docs/需求变更记录.md` 追加条目（需求 → 影响范围 → 数据版本号）。
2. **落码原则**：
   - 规格未定义处：代码内标 `TODO(SPEC-MISSING)`，降级行为按 §13；
   - 调参：只改 `algo/SimConfig`，不要散落常数；
   - 坐标：只改 `assets/layouts/*.json`（归一化值），禁止硬编码（§14-5）；
   - 新实体：`data/entity` + AppDatabase `version+1`（schema 自动导出到 `app/schemas/`）。
3. **测试**：算法变更必须在 `app/src/test` 补对应用例（algo 包为纯 Kotlin，无需真机）。
4. **数据更新**：英雄库走 CSV/JSON 导入（设置页），活动走手动 JSON（日历识别失败时 §13）。

## 构建环境备忘（本机）

- JDK：`D:\Environment\JDK`（JAVA_HOME 需指向此处，sdkmanager/gradle 均依赖）
- SDK：`C:\Users\24346\AppData\Local\Android\Sdk`（local.properties 已写入）
- Gradle 发行版：`C:\Users\24346\tools\gradle-8.7`；wrapper 缓存在 `~/.gradle/wrapper/dists`
- 真机部署：USB 连接 nova5 Pro → `./gradlew installDebug`，或 Android Studio Run

## 铁律自查（§14，每次发版前过一遍）

- [ ] 无 INTERNET 权限、无任何网络调用
- [ ] 无无障碍/模拟点击/注入/hook
- [ ] 识别不到显示 UNKNOWN，不猜（L4）
- [ ] 坐标全部走 LayoutProfile
- [ ] 无 Compose/Hilt/GMS 依赖
- [ ] 胜率只显示区间，不显示单点数字
- [ ] UI 显示信息完整度

# AutoClicker

**随机区域 · 随机间隔 · 智能连点 | Android 自动点击脚本**

[![License](https://img.shields.io/badge/License-MIT-green.svg)](https://github.com/Sun3299/AutoClicker/blob/main/LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-7F52FF.svg)](https://kotlinlang.org/)
[![Android SDK](https://img.shields.io/badge/Android%20SDK-26+-3DDC84.svg)](https://developer.android.com/)

> AutoClicker 是一款 Android 智能自动点击脚本工具，核心优势在于**随机区域点击**和**随机时间间隔**，模拟真人操作节奏，有效规避应用检测。配合悬浮窗控制和脚本管理，一款工具搞定所有重复性操作。

---

## ⭐ 核心亮点（区别于其他连点器）

| 亮点 | 说明 |
|------|------|
| 🎯 **随机区域** | 点击不再是一个固定坐标点，而是一个矩形范围，每次在该范围内**随机落点**，模拟真人点击，有效防止应用检测 |
| ⏱ **随机间隔** | 动作间隔支持设定 min~max 随机范围，每次执行都在区间内**随机取值**，告别机械节拍感 |
| 🎲 **滑动时长随机** | 滑动时长同样支持随机范围，让滑动操作也更自然 |
| 🪟 **悬浮窗独立控制** | 每个脚本都有专属悬浮控制窗口，可随时启停，无需切换页面 |
| ⏱ **三重停止策略** | 永不停止 / 按时长停止（时:分:秒）/ 按次数停止，灵活应对不同场景 |
| 🌙 **深色沉浸主题** | 护眼深色主题 + Material Design，卡片式脚本管理 |

---

## 功能详情

### 🎯 随机区域点击

传统连点器只能在**一个固定坐标**反复点击，容易被应用识别为机器人。

AutoClicker 的点击动作基于**矩形区域（Rect）** 定义，执行时在区域范围内**随机生成落点**：

```
区域定义（左上角 → 右下角）
  (x1, y1) ┌─────────────────┐
           │  ● ← 随机落点1  │
           │       ● ← 落点2 │   每次点击位置都不同
           │   ● ← 落点3     │
           │        ● ← 落点4 │
  (x2, y2) └─────────────────┘
```

配合随机间隔，组合出"真人感"极强的点击模式。

### ⏱ 随机间隔

动作之间的等待时间不再是固定值，而是 min ~ max 区间内的随机值：

```
动作A ──[随机等待 300~800ms]──▶ 动作B ──[随机等待 200~600ms]──▶ 动作C
```

### 👆 滑动 + 点击混合脚本

一个脚本可以同时包含多个**点击动作**和**滑动动作**，按顺序执行：

```
脚本「游戏连招」
  动作1：点击 [区域A]  间隔 100~300ms
  动作2：滑动 [区域B→C]  时长 200~400ms
  动作3：点击 [区域D]  间隔 150~350ms
  ...循环
```

### ⏱ 三种停止策略

| 模式 | 说明 | 适用场景 |
|------|------|----------|
| **永不停止** | 持续循环，直到手动停止 | 长时间挂机、监控 |
| **按时长停止** | 设置时分秒，到时自动停 | 限时任务 |
| **按次数停止** | 设置执行轮数，达到即停 | 批量操作 |

### 🪟 悬浮窗控制

每个脚本关联一个悬浮窗口，随时查看状态、快速启停。悬浮窗可跟随脚本切换，不影响当前操作。

### ⚙️ 灵活时间单位

时间参数支持三种单位自由切换，无需手动换算：
- **毫秒（ms）** — 精细控制
- **秒（s）** — 日常使用
- **分钟（min）** — 长间隔场景

---

## 界面预览

### 脚本列表

![UI 设计](ui_design.jpg)

### 运行效果

![运行示例](running_example.jpg)

---

## 系统架构

```
┌─────────────────────────────────────────────────────┐
│                     AutoClicker                      │
│                    Android App                        │
├─────────────────────────────────────────────────────┤
│  UI Layer（界面层）                                   │
│  ┌──────────────────┐  ┌──────────────────┐         │
│  │ MainActivity     │  │ ScriptListActivity│         │
│  │（主界面/脚本列表） │  │（脚本管理）       │         │
│  └────────┬─────────┘  └────────┬─────────┘         │
│           │                      │                    │
│  ┌────────┴──────────────────────┴────────┐         │
│  │ BottomSheet（动作编辑 / 设置 / 停止策略）  │         │
│  └──────────────────────────────────────────┘         │
├─────────────────────────────────────────────────────┤
│  Script Layer（脚本层）                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────┐ │
│  │ Script       │  │ScriptAction  │  │ Runtime   │ │
│  │（脚本实体）   │  │ Click/Swipe  │  │ Strategy  │ │
│  └──────────────┘  └──────────────┘  └──────────┘ │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────┐ │
│  │ScriptSettings│  │  MarkInfo    │  │  TimeUnit │ │
│  │（时间单位）   │  │（区域标记）   │  │（单位切换）│ │
│  └──────────────┘  └──────────────┘  └──────────┘ │
├─────────────────────────────────────────────────────┤
│  Executor Layer（执行层）                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────┐ │
│  │ClickExecutor │  │SwipeExecutor │  │ActionTask │ │
│  │（随机区域落点）│  │（滑动执行）   │  │（动作封装）│ │
│  └──────────────┘  └──────────────┘  └──────────┘ │
│  ┌────────────────────────┐  ┌─────────────────┐    │
│  │ ScriptScheduler         │  │ScriptAction     │    │
│  │（脚本调度器/循环执行）   │  │ Calculator      │    │
│  └────────────────────────┘  └─────────────────┘    │
├─────────────────────────────────────────────────────┤
│  Service Layer（服务层）                             │
│  ┌──────────────────────────────┐ ┌─────────────┐ │
│  │ AutoClickAccessibilityService │ │Permission   │ │
│  │（无障碍服务/模拟手势）         │ │Checker       │ │
│  └──────────────────────────────┘ └─────────────┘ │
├─────────────────────────────────────────────────────┤
│  Floating Window Layer（悬浮窗层）                   │
│  ┌──────────────────────────────┐                  │
│  │ FloatingWindowManager        │                  │
│  │（悬浮窗管理器/脚本独立控制）   │                  │
│  └──────────────────────────────┘                  │
└─────────────────────────────────────────────────────┘
```

---

## 项目结构

```
AutoClicker/
├── src/main/
│   ├── java/com/example/autoclicker/
│   │   ├── AutoClicker.kt                  # Application 类
│   │   │
│   │   ├── service/                        # 服务层
│   │   │   ├── AutoClickAccessibilityService.kt  # 无障碍服务（模拟手势）
│   │   │   └── PermissionChecker.kt              # 权限检查工具
│   │   │
│   │   ├── script/                         # 脚本层
│   │   │   ├── model/
│   │   │   │   ├── Script.kt              # 脚本实体
│   │   │   │   ├── ScriptAction.kt        # 动作模型（Click/Swipe 密封类）
│   │   │   │   ├── ScriptSettings.kt      # 脚本设置（时间单位）
│   │   │   │   ├── RuntimeStrategy.kt      # 停止策略（RunMode 密封类）
│   │   │   │   ├── MarkInfo.kt            # 区域标记信息
│   │   │   │   └── TimeUnit.kt            # 时间单位（毫秒/秒/分钟）
│   │   │   │
│   │   │   ├── executor/                  # 执行器
│   │   │   │   ├── ClickExecutor.kt       # 随机区域点击执行器
│   │   │   │   ├── SwipeExecutor.kt       # 滑动执行器
│   │   │   │   ├── ScriptScheduler.kt     # 脚本调度器（循环执行）
│   │   │   │   ├── ActionTask.kt          # 动作任务封装
│   │   │   │   └── ScriptActionCalculator.kt  # 参数验证与计算
│   │   │   │
│   │   │   ├── scheduler/                 # 调度器
│   │   │   └── ui/floating/              # 悬浮窗管理
│   │   │
│   │   └── ui/                           # 界面层
│   │       ├── activity/
│   │       │   ├── MainActivity.kt       # 主界面
│   │       │   └── ScriptListActivity.kt # 脚本列表
│   │       ├── component/                 # UI 组件
│   │       └── dialog/                   # 对话框
│   │
│   ├── res/
│   │   ├── layout/                        # 布局文件
│   │   ├── values/                       # 字符串/颜色/主题
│   │   ├── xml/
│   │   │   └── accessibility_service_config.xml  # 无障碍服务配置
│   │   ├── drawable/                     # 图标资源
│   │   └── mipmap-*/                     # 应用图标
│   │
│   └── AndroidManifest.xml
│
├── build.gradle.kts              # Gradle Kotlin DSL 构建脚本
├── proguard-rules.pro
└── .gitignore
```

---

## 核心代码片段

### 随机区域点击（ClickExecutor）

每次执行时，在矩形区域内随机生成落点：

```kotlin
// 点击区域定义（矩形）
data class Click(
    var clickRect: Rect,        // 左上(x1,y1) → 右下(x2,y2) 矩形区域
    var delayMinMs: Long = 0,   // 随机间隔最小值
    var delayMaxMs: Long = 500  // 随机间隔最大值
) : ScriptAction()

// 执行时：区域内随机落点
val realX = randomInRange(clickRect.left, clickRect.right)
val realY = randomInRange(clickRect.top, clickRect.bottom)
performClick(realX, realY)
```

### 随机滑动时长（SwipeExecutor）

```kotlin
data class Swipe(
    var startX: Int, var startY: Int,
    var endX: Int, var endY: Int,
    var durationMinMs: Long = 200,  // 时长随机最小值
    var durationMaxMs: Long = 500,  // 时长随机最大值
    var delayMinMs: Long = 0,        // 动作间隔随机最小值
    var delayMaxMs: Long = 500       // 动作间隔随机最大值
) : ScriptAction()
```

### 三种停止策略

```kotlin
sealed class RunMode {
    object NeverStop : RunMode()                    // 永不停止
    data class ByDuration(                          // 按时长停止
        val hour: Int = 0,
        val minute: Int = 0,
        val second: Int = 0
    ) : RunMode()
    data class ByCount(val times: Int = 10)        // 按次数停止
}
```

---

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| **语言** | Kotlin 1.9+ | Android 首选语言 |
| **Min SDK** | 26（Android 8.0） | 无障碍服务最低要求 |
| **Target SDK** | 36 | 最新 Android |
| **架构** | Clean Architecture | UI / Script / Executor / Service 分层 |
| **UI** | ViewBinding + Material Design | 类型安全视图绑定 |
| **构建** | Gradle 8.x Kotlin DSL | 现代构建系统 |

---

## 权限说明

| 权限 | 用途 |
|------|------|
| `BIND_ACCESSIBILITY_SERVICE` | 无障碍服务权限，模拟屏幕手势的核心权限 |
| `SYSTEM_ALERT_WINDOW` | 悬浮窗权限，显示和控制悬浮控制窗口 |

应用启动时自动检测权限，引导用户前往系统设置开启。

---

## 构建项目

```bash
# 克隆项目
git clone https://github.com/Sun3299/AutoClicker.git
cd AutoClicker

# Android Studio 打开项目，自动同步 Gradle

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK（需签名配置）
./gradlew assembleRelease
```

---

## 许可证

[MIT License](https://github.com/Sun3299/AutoClicker/blob/main/LICENSE)



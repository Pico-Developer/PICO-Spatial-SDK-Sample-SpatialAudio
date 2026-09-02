# Spatial Audio

[English](README.md) | [简体中文](README_zh.md)

Spatial Audio 是一款基于 PICO OS 6 的参考示例应用，使用 PICO Spatial SDK 演示空间音频功能。本示例从 Spatial Editor 资源包中加载 3D 场景，播放环境音频与基于对象的空间音频，并提供轻量级 UI 开关独立控制音频播放。

## 概览

Spatial Audio 呈现了一个带有飞翔小鸟与丛林氛围的沉浸式场景。沉浸式 Stage 渲染 3D 场景并通过 ECS 系统驱动小鸟运动，而平面控制面板窗口支持用户独立开关环境音频与小鸟音效。

场景从 `asset://editor-asset.bundle` 加载，由 `editor-asset/` 下的 PICO Spatial Editor 项目生成。音频文件从 Android assets 加载，并由生命周期感知的资源存储统一管理。

![Screenshot](.github/screenshot.jpeg)

## 功能特性

Spatial Audio 示例展示了 PICO Spatial SDK 的以下核心能力：

- **环境音频与对象空间音频**：背景环境音效，以及附着在小鸟实体上的移动声源
- **交互式开关**：独立控制环境音效与小鸟音效的开启/关闭
- **生命周期感知资源管理**：ViewModel 持有的音频资源存储（加载/缓存/释放）与播放控制器清理
- **程序化运动轨迹（ECS）**：带垂直起伏的环形飞行路径及朝向实时更新
- **编辑器到应用资源管线**：3D 场景在 Spatial Editor 中创作，打包为 `editor-asset.bundle` 供运行时加载

## 环境要求

构建和运行本项目前，请参照 PICO Spatial SDK 官方文档完成开发环境配置：

- [PICO Spatial SDK 环境配置指南（中国大陆）](https://developer-cn.picoxr.com/document/spatial-sdk/set-up-development-environment/)
- [PICO Spatial SDK 环境配置指南（非中国大陆）](https://developer.picoxr.com/document/spatial-sdk/set-up-development-environment/)

## 快速开始

### 1. 克隆项目

```bash
git clone <repository-url>
cd SpatialAudio
```

### 2. 在 Android Studio 中打开项目

启动 Android Studio，选择 **Open an Existing Project**，然后选择 `SpatialAudio` 目录。

### 3. 运行项目

#### 在模拟器上运行

1. 打开 **Device Manager**（可通过工具栏或 **Tools > Device Manager** 进入）
2. 点击 **Add a new device** > **Create a PICO Emulator**
3. 在 **Device Manager** 中启动模拟器
4. 点击 **Run 'app'** 启动应用

#### 在物理设备上运行

1. 将 PICO 设备连接至开发机
2. 在设备上启用 **USB 调试（USB Debugging）**
3. 点击 **Run 'app'** 将应用直接部署到设备

## 项目结构

Spatial Audio 示例采用模块化架构，将应用逻辑与 3D 资源分离：

```txt
SpatialAudio/
├── app/                                               # 主应用模块
│   └── src/main/
│       ├── java/com/pico/spatial/sample/spatialaudio/ # Kotlin 源代码
│       │   ├── Main.kt                                # 应用入口：Stage + 控制面板，启动预加载
│       │   ├── data/
│       │   │   ├── AssetBundle.kt                     # 全局懒加载 `asset://editor-asset.bundle`
│       │   │   └── AudioResourceStore.kt              # 音频名称/路径注册表与资源加载/缓存/释放
│       │   ├── ecs/
│       │   │   └── FlyTrajectory.kt                   # 小鸟轨迹组件与 ECS 系统（变换更新）
│       │   ├── manager/
│       │   │   ├── AudioPlayer.kt                     # 环境循环播放与随机小鸟音效，控制器清理
│       │   │   └── EventManager.kt                    # 音频事件订阅（由 ViewModel 持有生命周期）
│       │   ├── platform/                              # 平台引导（Application/Activity）
│       │   └── ui/
│       │       ├── AudioControlViewModel.kt           # 场景加载、音频节点准备、状态管理与清理
│       │       ├── ControlPanel.kt                    # 环境音效与小鸟音效的 UI 开关
│       │       └── ImmersiveScene.kt                  # 根实体、场景加载钩子与 ECS 注册
│       ├── assets/audio/                              # WAV 音频资源（环境音效与小鸟音效）
│       ├── res/                                       # Android 资源
│       └── AndroidManifest.xml                        # 包含 Spatial 组件声明的应用清单
├── editor-asset/                                      # 3D 资源与场景定义
│   └── src/main/res3d/PicoSpatialAudio/               # Spatial Editor 项目
│       ├── Sources/Assets/                            # 3D 模型、贴图与音频
│       └── Sources/Scenes/                            # 场景定义（SpatialAudioScene.usda）
├── gradle/                                            # Gradle 依赖与构建脚本
├── build.gradle.kts                                   # 根构建配置
└── settings.gradle.kts                                # 项目设置与 SDK 配置
```

## 音频资源

音频资源位于 `app/src/main/assets/audio/`。如需添加或替换音频文件：

1. 将 WAV 文件添加至 `app/src/main/assets/audio/`
2. 在 `AudioResourceStore.kt` 中注册名称与路径（更新 `BIRD_AUDIO_NAME_PATH_LIST` 和/或 `AMBIENT_AUDIO_NAME`）
3. 如需在场景编辑时使用，可将相同文件复制至 Spatial Editor 项目目录（`editor-asset/src/main/res3d/PicoSpatialAudio/Sources/Assets/audio/`）

## 编辑 3D 资源

3D 资源在 `editor-asset/src/main/res3d/PicoSpatialAudio/` 下的 Spatial Editor 项目中创作。如需修改或扩展，请按以下步骤操作：

1. 在 Android Studio 中展开 **editor-asset** 模块
2. 导航至 **res3d/PicoSpatialAudio/ModelView**
3. 点击右上角的 **Open in Editor** 启动 PICO Spatial Editor
4. 编辑场景（替换模型/贴图或调整 `SpatialAudioScene` 内容）
5. 构建项目以重新生成 `editor-asset.bundle`；应用在运行时通过 `asset://editor-asset.bundle` 加载

![Spatial Editor](.github/editor.png)

## 学习资源

深入了解空间应用开发，欢迎访问官方开发者网站获取完整资源：

- [PICO 开发者网站（中国大陆）](https://developer-cn.picoxr.com/)
- [PICO 开发者网站（非中国大陆）](https://developer.picoxr.com/)

## 许可证

© 2026 PICO。本项目的源代码依据 [Apache 2.0 License](LICENSE.txt) 协议开源。本项目的美术资产（包括但不限于纹理、3D 模型和音频）基于 [Creative Commons BY 4.0](https://creativecommons.org/licenses/by/4.0/) 授权。

## 支持

如有任何问题、疑惑或功能建议，欢迎通过 [PICO 开发者支持门户](https://picodevsupport.freshdesk.com/support/home) 提交工单与我们联系。

---

为空间应用开发者提供的官方参考示例

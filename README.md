# Notebot (Minecraft 26.3 / Fabric)

仓库：<https://github.com/Sehhan667/notebot>

MeteorClient 的 Notebot 功能独立移植版：一个带曲库、收藏夹、播放队列与三种发声方式的游戏内音乐播放器。

原始项目：[MeteorDevelopment/meteor-client](https://github.com/MeteorDevelopment/meteor-client)（GPL-3.0）。
本模组是它的衍生作品，同样以 **GPL-3.0** 发布。

## 环境

| 项 | 版本 |
|---|---|
| Minecraft | 26.3 |
| Fabric Loader | 0.19.5+ |
| Fabric API | 0.165.2+26.3（必需） |
| Java | 25 |

## 安装

1. 安装 Fabric Loader 0.19.5 与 Fabric API。
2. 把 `notebot-0.1.0.jar` 放进 `mods/`。
3. 启动游戏，在「选项 → 控制 → 按键绑定」里给 **Open Notebot menu** 绑定一个键（默认不绑定，避免撞键）。

## 使用

- 曲库目录：`.minecraft/notebot`，**支持任意层级子文件夹**，用于分类整理。
- 支持格式：`.nbs`（Note Block Studio）与 `.txt`（`tick:音级:乐器序号`，Meteor 旧格式）。
- 曲库页：面包屑导航、文件夹、搜索、收藏（`*` 标记）、`Reload` 重扫。
- 播放页：上一首 / 播放暂停 / 下一首 / 停止、可拖动进度条、时间显示、
  循环模式（顺序 / 列表循环 / 单曲循环 / 随机）、音量、输出后端切换。
- 四种循环模式在预览与随身模式下即时生效；音符盒模式切换歌曲会重新扫描调音。

### 三种输出后端

| 后端 | 原理 | 前置条件 |
|---|---|---|
| Preview | 客户端 `playSound`，本地即时 | 无 |
| Noteblock | 扫描附近音符盒 → 调音 → 击打包 | 交互距离内有可用音符盒、生存模式 |
| Portable | 逐音符发送 `/playsound` | 权限等级 2（op / 开作弊） |

`Portable` 会把声音播给附近玩家（`@a` + 玩家坐标，带距离衰减），
命令反馈会按 `commands.playsound.success` 翻译键在客户端屏蔽；
一旦检测到权限不足会立即停止并切回 `Preview`。
密集曲目受「每 tick 命令上限」限制（默认 8，可在设置里调）。

## 设置

设置页分两个 Tab：

- **General**：tick delay、并发调音数、模式（任意/精确乐器）、乐器检测方式、
  复调、自动转头、挥手、超范围移调、复查延迟，以及 Portable 分组。
- **Note map**：16 种乐器各自映射到另一种乐器（仅「精确乐器」模式生效）。

配置保存在 `.minecraft/config/notebot.json`。

## 构建

需要 JDK 25（本机为 `E:\MinecraftMods\tools\jdk-25`）与 Gradle wrapper。
项目使用 Unimined（26.3 无混淆，无需 Yarn/Intermediary）。

```powershell
$env:JAVA_HOME='E:\MinecraftMods\tools\jdk-25'
.\gradlew.bat build
# 产物：build/libs/notebot-0.2.jar
```

## 已知限制

- **没有 3D 提示渲染**：26.3 渲染重构后这版 Fabric API 不再提供世界渲染事件，
  实现它需要 mixin；为保持零 mixin，相关设置项已整体移除。
- **界面文案目前是英文硬编码**，仅按键名做了 `en_us` / `zh_cn` 本地化。
- 音符盒模式未做真人手感验证；服务器插件/反作弊对发包的容忍度需实测。
- 随身后端在高密度曲目下可能触发服务器限流，建议调低「每 tick 命令上限」。

## 作者

- AnotherCream
- DeepSeek

Notebot 逻辑派生自 [MeteorClient](https://github.com/MeteorDevelopment/meteor-client)。

## 许可

GPL-3.0。Notebot 逻辑派生自 MeteorClient，保留其版权声明。

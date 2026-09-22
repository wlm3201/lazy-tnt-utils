# Lazy TNT Utils（弱加载 TNT 可视化）

**简体中文** | [English](README_EN.md)

一个 Minecraft 的 Fabric mod，在弱加载区块中准确显示 TNT 与物品实体的真实状态，主要面向弱加载 TNT 装置开发。支持 26.1.2 与 26.2。

---

## 解决问题

弱加载区块里服务端不会 tick 实体，即 TNT 既不会递减引信也不会自行运动，但客户端仍照常本地模拟，导致客户端把 TNT 引信跑完，提前爆炸并消失。

---

## 工作原理

本 mod 为客户端 mod，服务端可选安装：

- **客户端**：单人游戏把受支持实体逐帧修正为集成服务端真实状态，多人游戏则取消客户端本地模拟，从而 TNT 不会提前爆炸 / 自行运动。

- **服务端**：服务端逐帧采集玩家附近 160 格内的实体真实状态（位置 / 速度 / 引信），经自定义网络包发给该玩家；客户端缓存后修正本地实体。默认关闭。

### 支持的实体类别

| 类别     | 命令名         | 覆盖实体                                                                                       |
| -------- | -------------- | ---------------------------------------------------------------------------------------------- |
| TNT      | `tnt`          | `PrimedTnt`                                                                                    |
| 物品     | `item`         | `ItemEntity`                                                                                   |
| 弹射物   | `projectile`   | `Projectile` 全部子类（箭 / 三叉戟 / 雪球 / 珍珠 / 药水 / 火球 / 风弹 / 潜影弹 / 烟花 / 鱼钩） |
| 下落方块 | `fallingblock` | `FallingBlockEntity`                                                                           |
| 经验球   | `xp`           | `ExperienceOrb`                                                                                |

---

## 功能特性

- ✅ 弱加载区块 TNT 不再提前爆炸 / 自行运动。
- ✅ 弱加载区块实体分离渲染位置同步。
- ✅ F3+B 为 TNT 绘制动量箭头。
- ✅ 隐藏实体朝向箭头。
- ✅ 可通过命令开关。
- ✅ 精确显示爆炸粒子。
- ✅ 关闭 TNT 闪烁与变大视效。
- ✅ 显示 TNT 剩余爆炸刻数标签。
- ✅ 修改渲染距离与模拟距离。
- ✅ 解除修改 Motion 的 ±10 限制。

---

## 命令开关

所有开关都在**客户端命令** `/lazytntutils` 中管理：

```
/lazytntutils                                                                      # 查询配置
/lazytntutils <tnt|item|projectile|fallingblock|xp> <client|server> <true|false>   # 同步开关
/lazytntutils <view|sim> [default|<0..32>]                                         # 修改视距 / 模距
/lazytntutils tnt <visual|timer> [true|false]                                      # 开关视效 / 标签
/lazytntutils <tnt|projectile> arrow [arrow|line|off]                              # 动量矢量画法
/lazytntutils facing [true|false]                                                  # 开关朝向箭头
```

- `client` 开关在客户端本地即时生效，写入 `config/lazytntutils-client.properties`。
- `server` 开关经网络包交给服务端落地，服务端持久化到 `config/lazytntutils.properties` 并向所有客户端广播。

---

## 构建（开发者）

仓库是多版本结构：源码只有根目录一份（`src/main` + `src/client`），每个 MC 版本对应一个子项目目录，
子项目只放 `gradle.properties`（声明 mc / loader / fabric-api / java 版本）。

```powershell
# 构建全部版本（Windows）
.\gradlew.bat buildAll

# 只构建某个版本
.\gradlew.bat :v26_1_2:build
```

产物位于对应子项目，如 `v26_1_2/build/libs/lazytntutils-1.0.3-26.1.2.jar`。

构建需 **Java 25**（见各版本 `gradle.properties` 的 `javaRelease`）。

新增 MC 版本：复制任一 `v26_x/gradle.properties` 到新目录、改其中 4 个值，并在 `settings.gradle` 里 `include` 即可，源码无需拷贝。

---

## 性能与已知限制

同步为追求精确**每 tick 全量发送**玩家附近所有受支持实体（每颗 TNT ≈ 65 字节，其余类别 ≈ 61 字节）。量级估算（每玩家、20 tps）：

- 单颗 TNT：约 **1.3 KB/s**，多玩家线性叠加。
- 作为对比，原版同步是相对增量 + 节流，同场景约为本 mod 的 **1/10 到 0**。
- tick sprint 时会暂停同步。

> 带宽只在**专用多人服**有意义；单人走 loopback，无真实网络消耗。

---

## 许可证

[MIT](LICENSE) © wlm3201

# Lazy TNT Utils（弱加载 TNT 可视化）

[English](README_EN.md) | **简体中文**

一个 Minecraft 的 Fabric mod，在弱加载区块中准确显示 TNT 与物品实体的真实状态，主要面向弱加载TNT装置开发。支持 26.1.2 与 26.2。

---

## 解决问题

弱加载区块里服务端不会 tick 实体：TNT 引信不递减、物品不下落；但客户端仍照常本地模拟，导致：

- **TNT**：客户端把引信跑完，提前爆炸并消失，与服务端脱节。
- **物品**：客户端自行运动，与服务端错位。

---

## 工作原理

本 mod 为客户端 mod ，服务端可选安装：

- **客户端**：单人游戏把TNT/物品状态逐帧修正为集成服务端真实状态，多人游戏则取消客户端本地模拟，从而不会提前爆炸/自行运动。默认 **tnt** 开启，**item** 关闭。

- **服务端**：服务端逐帧采集玩家附近 160 格内的TNT/物品真实状态（位置/速度/引信），经自定义网络包发给该玩家；客户端缓存后修正本地实体。默认关闭。

---

## 功能特性

- ✅ 弱加载区块TNT不再"客户端提前爆炸消失"。
- ✅ 弱加载区块物品不再"客户端自行运动"。
- ✅ 弱加载区块实体分离渲染位置同步。
- ✅ 显示碰撞箱时为TNT绘制动量箭头/线段。
- ✅ 可通过命令开关。
- ✅ 精确显示爆炸粒子。
- ✅ 关闭TNT闪烁与变大视觉效果。
- ✅ 显示TNT剩余爆炸刻数标签。
- ✅ 修改渲染距离与模拟距离。
- ✅ 解除原版Motion的±10限制。

---

## 命令开关

所有开关都在**客户端命令** `/lazytntutils` 中管理：

```
/lazytntutils                                              # 查询配置
/lazytntutils <tnt|item> <client|server> <true|false>      # 同步开关
/lazytntutils <view|sim> [default|<0..32>]                 # 修改视距/模距
/lazytntutils tnt <visual|timer> [true|false]              # 开关视效/标签
/lazytntutils tnt arrow [arrow|line|off]                   # 动量矢量画法
```

- `client` 开关在客户端本地即时生效，写入 `config/lazytntutils-client.properties`。
- `server` 开关经网络包交给服务端落地，服务端持久化到 `config/lazytntutils.properties` 并向所有客户端广播。
- `view` / `sim` 只改运行期覆盖值，不写入任何配置文件：重启服务端、或单人下退出存档即恢复原版设置（避免与 `server.properties` 形成两个"谁优先"的来源）。

## 构建（开发者）

26.x 全系使用官方 Mojang 命名，源码**只在仓库根目录 `src/main` 维护一份**，各 MC 版本通过子项目的 `gradle.properties` 切换依赖。当前含子项目 `v26_1_2`（26.1.2）与 `v26_2`（26.2）。

```powershell
# 构建全部版本（Windows）
.\gradlew.bat buildAll

# 构建全部版本（Linux / macOS）
./gradlew buildAll

# 只构建某个版本
.\gradlew.bat :v26_1_2:build
```

产物位于对应子项目，如 `v26_1_2/build/libs/lazytntutils-26.1.2.jar`。

**新增 / 切换版本**：在 `settings.gradle` 加 `include 'vXX'`，新建 `vXX/gradle.properties`（照抄现成子项目，仅改 `mc`、`loader`、`fapi`、`jarName`，必要时改 `javaRelease`）即可。

构建需 **Java 25**（见各版本 `gradle.properties` 的 `javaRelease`）。

---

## 性能与已知限制

同步为追求精确**每 tick 全量发送**玩家附近所有 TNT / 物品（每颗 TNT ≈ 65 字节，物品 ≈ 61 字节）。量级估算（每玩家、20 tps）：

- 单颗 TNT：约 **1.3 KB/s**，多玩家线性叠加。
- 作为对比，原版同步是相对增量 + 节流，同场景约为本 mod 的 **1/10 到 0**。
- tick sprint时会暂停同步。

> 带宽只在**专用多人服**有意义；单人走 loopback，无真实网络消耗。

---

## 许可证

[MIT](LICENSE) © wlm3201
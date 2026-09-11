package net.orbitalstrike.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.server.IntegratedServer;
import net.orbitalstrike.SyncConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 单人（集成服务端）下让视距 / 模拟距离的覆盖值不被原版每 tick 抢回去。
 *
 * <p>原版 {@code IntegratedServer.tickServer} 末尾会把两个距离同步成客户端视频设置，但两者的 "是否要改"判据不一样：
 *
 * <pre>
 * int view = Math.max(2, this.minecraft.options.renderDistance().get());
 * if (view != this.getPlayerList().getViewDistance()) {          // 比对"服务端当前值"
 *   this.getPlayerList().setViewDistance(view);
 * }
 * int sim = Math.max(2, this.minecraft.options.simulationDistance().get());
 * if (sim != this.previousSimulationDistance) {                  // 比对"自己缓存的字段"
 *   this.getPlayerList().setSimulationDistance(sim);
 *   this.previousSimulationDistance = sim;
 * }
 * </pre>
 *
 * <p>视距比的是 PlayerList 的当前值，所以本 mod 改完下一 tick 就被还原（且原版会逐 tick 打 INFO 日志）；模拟距离比的是 {@code
 * previousSimulationDistance} 这个缓存字段，mod 改 PlayerList 不会
 * 动它，因此不会被还原。这正是"专用服两个都生效，单人只有模拟距离生效、视距跟着视频设置走"的原因。
 *
 * <p>做法：把这两处 {@code Math.max} 的返回值换成覆盖值（-1 = 不覆盖时保持原版）。于是由原版自己 下发我们的值，比较相等后既不重复广播也不刷日志；覆盖值清掉（{@code
 * default}）后，下一 tick 原版 又会自动还原成视频设置的值。顺带绕开了原版 {@code max(2, ...)} 的下限，使 0 / 1 也能作为真实值生效。
 *
 * <p>两个 {@code Math.max} 在字节码里顺序固定：ordinal 0 = 视距，ordinal 1 = 模拟距离（26.1.2 与 26.2 均已核对）。
 *
 * <p>本 mixin 只在单人成立：它直接读 {@link SyncConfig} 的静态覆盖值，依赖"集成服务端与客户端同进程"这一前提（多人下服务端在别的进程，
 * 覆盖值由服务端自己应用，不需要这里介入）。覆盖值的生命周期见 SyncConfig 距离字段，关服时被 {@code resetDistance()} 清掉。
 */
@Environment(EnvType.CLIENT)
@Mixin(IntegratedServer.class)
public class IntegratedServerMixin {

  @Redirect(
      method = "tickServer",
      at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I", ordinal = 0))
  private static int lazytnt$overrideViewDistance(int a, int b) {
    int override = SyncConfig.viewDistanceOverride;
    return override >= 0 ? override : Math.max(a, b);
  }

  @Redirect(
      method = "tickServer",
      at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I", ordinal = 1))
  private static int lazytnt$overrideSimulationDistance(int a, int b) {
    int override = SyncConfig.simulationDistanceOverride;
    return override >= 0 ? override : Math.max(a, b);
  }
}

package com.lazytntutils.client.mixin;

import com.lazytntutils.SyncConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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

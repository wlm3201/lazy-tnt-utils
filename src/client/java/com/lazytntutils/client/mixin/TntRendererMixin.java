package com.lazytntutils.client.mixin;

import com.lazytntutils.client.ClientSyncConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.TntRenderer;
import net.minecraft.client.renderer.entity.state.TntRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(TntRenderer.class)
public class TntRendererMixin {

    private static final float NEUTRAL_FUSE = 85.0F;

    private static final Vec3 NAME_TAG_ATTACHMENT = new Vec3(0.0, 1.0, 0.0);

    private static final double NAME_TAG_DISTANCE_SQ = 64.0 * 64.0;

    private static final String TICK_LABEL_KEY = "lazytntutils.hud.tntFuse";

    private static final int MAX_LABEL_CACHE = 512;

    private static final Map<Integer, Component> LABEL_CACHE = new HashMap<>();

    private static final Map<PrimedTnt, Integer> LAST_FUSE = new WeakHashMap<>();

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void lazytntutils$applyTntVisuals(
            PrimedTnt entity, TntRenderState state, float partialTicks, CallbackInfo ci) {
        int fuse = entity.getFuse();

        if (ClientSyncConfig.tntTickTimer && state.distanceToCameraSq <= NAME_TAG_DISTANCE_SQ) {
            state.nameTagAttachment = NAME_TAG_ATTACHMENT;
            state.nameTag = tickLabel(fuse);
        }

        if (ClientSyncConfig.tntNoFlashScale) {
            state.fuseRemainingInTicks = NEUTRAL_FUSE;
            return;
        }

        Integer last = LAST_FUSE.get(entity);
        if (last != null && last.intValue() == fuse) {
            state.fuseRemainingInTicks = fuse + 1.0F;
        }
        LAST_FUSE.put(entity, fuse);
    }

    private static Component tickLabel(int fuse) {
        Component cached = LABEL_CACHE.get(fuse);
        if (cached != null) return cached;
        if (LABEL_CACHE.size() >= MAX_LABEL_CACHE) LABEL_CACHE.clear();
        Component built =
                Component.translatable(TICK_LABEL_KEY, fuse)
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(colorForFuse(fuse))));
        LABEL_CACHE.put(fuse, built);
        return built;
    }

    private static int colorForFuse(int fuse) {
        if (fuse < 20) return 0xFF0000;
        if (fuse < 40) return 0xFF8000;
        return 0xFFFFFF;
    }
}

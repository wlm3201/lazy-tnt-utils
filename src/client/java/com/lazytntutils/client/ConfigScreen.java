package com.lazytntutils.client;

import com.lazytntutils.SyncConfig;
import com.lazytntutils.network.SyncConfigC2SPayload;
import com.lazytntutils.network.SyncDistanceC2SPayload;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ConfigScreen {

    private ConfigScreen() {}

    private static final String KEY = "lazytntutils.config.";

    private record TrackedKind(String kind) {}

    private static final List<TrackedKind> KINDS =
            List.of(
                    new TrackedKind("tnt"),
                    new TrackedKind("item"),
                    new TrackedKind("projectile"),
                    new TrackedKind("fallingblock"),
                    new TrackedKind("xp"));

    public static Screen create(Screen parent) {
        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable(KEY + "title"))
                .category(syncCategory())
                .category(renderCategory())
                .category(distanceCategory())
                .save(ClientSyncConfig::save)
                .build()
                .generateScreen(parent);
    }

    private static ConfigCategory syncCategory() {
        ConfigCategory.Builder builder =
                ConfigCategory.createBuilder().name(Component.translatable(KEY + "category.sync"));

        for (TrackedKind kind : KINDS) {
            builder.group(
                    OptionGroup.createBuilder()
                            .name(Component.translatable(KEY + "group." + kind.kind()))
                            .option(clientSyncOption(kind))
                            .option(serverSyncOption(kind))
                            .build());
        }
        return builder.build();
    }

    private static ConfigCategory renderCategory() {
        return ConfigCategory.createBuilder()
                .name(Component.translatable(KEY + "category.render"))
                .option(
                        boolOption(
                                KEY + "option.tickTimer",
                                1,
                                true,
                                () -> ClientSyncConfig.tntTickTimer,
                                v -> ClientSyncConfig.tntTickTimer = v))
                .option(
                        boolOption(
                                KEY + "option.noFlashScale",
                                1,
                                true,
                                () -> ClientSyncConfig.tntNoFlashScale,
                                v -> ClientSyncConfig.tntNoFlashScale = v))
                .option(
                        arrowOption(
                                KEY + "option.tntArrow",
                                () -> ClientSyncConfig.tntMotionArrow,
                                v -> ClientSyncConfig.tntMotionArrow = v))
                .option(
                        arrowOption(
                                KEY + "option.projectileArrow",
                                () -> ClientSyncConfig.projectileMotionArrow,
                                v -> ClientSyncConfig.projectileMotionArrow = v))
                .option(
                        boolOption(
                                KEY + "option.explosionParticles",
                                1,
                                true,
                                () -> ClientSyncConfig.simplifyExplosionParticles,
                                v -> ClientSyncConfig.simplifyExplosionParticles = v))
                .option(
                        boolOption(
                                KEY + "option.facingArrow",
                                1,
                                false,
                                () -> ClientSyncConfig.showFacingArrow,
                                v -> ClientSyncConfig.showFacingArrow = v))
                .build();
    }

    private static ConfigCategory distanceCategory() {
        return ConfigCategory.createBuilder()
                .name(Component.translatable(KEY + "category.distance"))
                .option(distanceOption("view", KEY + "option.viewDistance"))
                .option(distanceOption("sim", KEY + "option.simDistance"))
                .build();
    }

    private static Option<Boolean> clientSyncOption(TrackedKind kind) {
        return boolOption(
                KEY + "option.clientSync",
                1,
                ClientSyncConfig.defaultClientSync(kind.kind()),
                () -> clientFlag(kind.kind()),
                v -> setClientFlag(kind.kind(), v));
    }

    private static Option<Boolean> serverSyncOption(TrackedKind kind) {
        return boolOption(
                KEY + "option.serverSync",
                1,
                false,
                () -> serverFlag(kind.kind()),
                v -> setServerFlag(kind.kind(), v));
    }

    private static Option<Boolean> boolOption(
            String key, int descCount, boolean def, Supplier<Boolean> get, Consumer<Boolean> set) {
        return Option.<Boolean>createBuilder()
                .name(Component.translatable(key))
                .description(description(key, descCount))
                .binding(def, get, set)
                .controller(BooleanControllerBuilder::create)
                .build();
    }

    private static Option<MotionArrowMode> arrowOption(
            String key, Supplier<MotionArrowMode> get, Consumer<MotionArrowMode> set) {
        return Option.<MotionArrowMode>createBuilder()
                .name(Component.translatable(key))
                .description(description(KEY + "option.arrow", 1))
                .binding(MotionArrowMode.ARROW, get, set)
                .controller(
                        opt ->
                                EnumControllerBuilder.create(opt)
                                        .enumClass(MotionArrowMode.class)
                                        .formatValue(
                                                mode ->
                                                        Component.translatable(
                                                                mode.translationKey())))
                .build();
    }

    private static Option<Integer> distanceOption(String kind, String key) {
        return Option.<Integer>createBuilder()
                .name(Component.translatable(key))
                .description(description(KEY + "option.distance", 1))
                .binding(-1, () -> distance(kind), v -> setDistance(kind, v))
                .controller(
                        opt ->
                                IntegerSliderControllerBuilder.create(opt)
                                        .range(-1, SyncConfig.MAX_DISTANCE)
                                        .step(1)
                                        .formatValue(ConfigScreen::distanceText))
                .build();
    }

    private static Component distanceText(Integer value) {
        return value < 0
                ? Component.translatable(KEY + "distance.default")
                : Component.translatable(KEY + "distance.chunks", value);
    }

    private static OptionDescription description(String key, int count) {
        Component[] lines = new Component[count];
        for (int i = 1; i <= count; i++) {
            lines[i - 1] = Component.translatable(key + ".desc." + i);
        }
        return OptionDescription.of(lines);
    }

    private static boolean clientFlag(String kind) {
        return switch (kind) {
            case "tnt" -> ClientSyncConfig.tntClientSync;
            case "item" -> ClientSyncConfig.itemClientSync;
            case "projectile" -> ClientSyncConfig.projectileClientSync;
            case "fallingblock" -> ClientSyncConfig.fallingBlockClientSync;
            case "xp" -> ClientSyncConfig.experienceOrbClientSync;
            default -> false;
        };
    }

    private static void setClientFlag(String kind, boolean on) {
        switch (kind) {
            case "tnt" -> ClientSyncConfig.tntClientSync = on;
            case "item" -> ClientSyncConfig.itemClientSync = on;
            case "projectile" -> ClientSyncConfig.projectileClientSync = on;
            case "fallingblock" -> ClientSyncConfig.fallingBlockClientSync = on;
            case "xp" -> ClientSyncConfig.experienceOrbClientSync = on;
            default -> throw new IllegalArgumentException("未知类别：" + kind);
        }
    }

    private static boolean serverFlag(String kind) {
        return switch (kind) {
            case "tnt" -> ClientSyncConfig.tntServerSync;
            case "item" -> ClientSyncConfig.itemServerSync;
            case "projectile" -> ClientSyncConfig.projectileServerSync;
            case "fallingblock" -> ClientSyncConfig.fallingBlockServerSync;
            case "xp" -> ClientSyncConfig.experienceOrbServerSync;
            default -> false;
        };
    }

    private static void setServerFlag(String kind, boolean on) {
        switch (kind) {
            case "tnt" -> ClientSyncConfig.tntServerSync = on;
            case "item" -> ClientSyncConfig.itemServerSync = on;
            case "projectile" -> ClientSyncConfig.projectileServerSync = on;
            case "fallingblock" -> ClientSyncConfig.fallingBlockServerSync = on;
            case "xp" -> ClientSyncConfig.experienceOrbServerSync = on;
            default -> throw new IllegalArgumentException("未知类别：" + kind);
        }
        if (ClientPlayNetworking.canSend(SyncConfigC2SPayload.ID)) {
            ClientPlayNetworking.send(new SyncConfigC2SPayload(kind, on));
        }
    }

    private static int distance(String kind) {
        return kind.equals("view")
                ? ClientSyncConfig.viewDistanceOverride
                : ClientSyncConfig.simulationDistanceOverride;
    }

    private static void setDistance(String kind, int value) {
        int clamped = SyncConfig.clampDistance(value);
        if (kind.equals("view")) ClientSyncConfig.viewDistanceOverride = clamped;
        else ClientSyncConfig.simulationDistanceOverride = clamped;
        if (ClientPlayNetworking.canSend(SyncDistanceC2SPayload.ID)) {
            ClientPlayNetworking.send(new SyncDistanceC2SPayload(kind, clamped));
        }
    }
}

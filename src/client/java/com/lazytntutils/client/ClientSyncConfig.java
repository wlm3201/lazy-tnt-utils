package com.lazytntutils.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

public final class ClientSyncConfig {

    private ClientSyncConfig() {}

    private static final boolean DEFAULT_TNT_CLIENT_SYNC = true;

    public static boolean tntClientSync = DEFAULT_TNT_CLIENT_SYNC;

    public static boolean itemClientSync = false;

    public static boolean projectileClientSync = false;

    public static boolean fallingBlockClientSync = false;

    public static boolean experienceOrbClientSync = false;

    public static boolean tntNoFlashScale = true;

    public static boolean tntTickTimer = true;

    public static MotionArrowMode tntMotionArrow = MotionArrowMode.ARROW;

    public static MotionArrowMode projectileMotionArrow = MotionArrowMode.ARROW;

    public static boolean showFacingArrow = false;

    public static boolean simplifyExplosionParticles = true;

    public static boolean defaultClientSync(String kind) {
        return switch (kind) {
            case "tnt" -> DEFAULT_TNT_CLIENT_SYNC;
            default -> false;
        };
    }

    public static boolean tntServerSync = false;

    public static boolean itemServerSync = false;

    public static boolean projectileServerSync = false;

    public static boolean fallingBlockServerSync = false;

    public static boolean experienceOrbServerSync = false;

    public static boolean serverPaused = false;

    public static int viewDistanceOverride = -1;

    public static int simulationDistanceOverride = -1;

    private static Path configFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("lazytntutils-client.properties");
    }

    public static void load() {
        Path file = configFile();
        if (!Files.exists(file)) return;
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        } catch (IOException e) {
            LazyTNTUtilsClient.LOGGER.warn("[LazyTNTUtils] 读取客户端配置文件失败，使用默认配置", e);
            return;
        }
        tntClientSync = parse(props, "tntClientSync", tntClientSync);
        itemClientSync = parse(props, "itemClientSync", itemClientSync);
        projectileClientSync = parse(props, "projectileClientSync", projectileClientSync);
        fallingBlockClientSync = parse(props, "fallingBlockClientSync", fallingBlockClientSync);
        experienceOrbClientSync = parse(props, "experienceOrbClientSync", experienceOrbClientSync);
        tntNoFlashScale = parse(props, "tntNoFlashScale", tntNoFlashScale);
        tntTickTimer = parse(props, "tntTickTimer", tntTickTimer);
        tntMotionArrow =
                MotionArrowMode.fromId(
                        props.getProperty("tntMotionArrow", MotionArrowMode.ARROW.id()));
        projectileMotionArrow =
                MotionArrowMode.fromId(
                        props.getProperty("projectileMotionArrow", MotionArrowMode.ARROW.id()));
        showFacingArrow = parse(props, "showFacingArrow", showFacingArrow);
        simplifyExplosionParticles =
                parse(props, "simplifyExplosionParticles", simplifyExplosionParticles);
    }

    private static boolean parse(Properties props, String key, boolean fallback) {
        String v = props.getProperty(key);
        return v == null ? fallback : Boolean.parseBoolean(v.trim());
    }

    public static void save() {
        Properties props = new Properties();
        props.setProperty("tntClientSync", Boolean.toString(tntClientSync));
        props.setProperty("itemClientSync", Boolean.toString(itemClientSync));
        props.setProperty("projectileClientSync", Boolean.toString(projectileClientSync));
        props.setProperty("fallingBlockClientSync", Boolean.toString(fallingBlockClientSync));
        props.setProperty("experienceOrbClientSync", Boolean.toString(experienceOrbClientSync));
        props.setProperty("tntNoFlashScale", Boolean.toString(tntNoFlashScale));
        props.setProperty("tntTickTimer", Boolean.toString(tntTickTimer));
        props.setProperty("tntMotionArrow", tntMotionArrow.id());
        props.setProperty("projectileMotionArrow", projectileMotionArrow.id());
        props.setProperty("showFacingArrow", Boolean.toString(showFacingArrow));
        props.setProperty(
                "simplifyExplosionParticles", Boolean.toString(simplifyExplosionParticles));
        Path file = configFile();
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream out = Files.newOutputStream(file)) {
                props.store(out, "LazyTNTUtils client sync config");
            }
        } catch (IOException e) {
            LazyTNTUtilsClient.LOGGER.warn("[LazyTNTUtils] 写入客户端配置文件失败", e);
        }
    }

    public static void reset() {
        tntServerSync = false;
        itemServerSync = false;
        projectileServerSync = false;
        fallingBlockServerSync = false;
        experienceOrbServerSync = false;
        serverPaused = false;
        viewDistanceOverride = -1;
        simulationDistanceOverride = -1;
    }
}

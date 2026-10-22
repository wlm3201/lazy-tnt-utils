package com.lazytntutils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

public final class SyncConfig {

    private SyncConfig() {}

    public static boolean tntServerSync = false;

    public static boolean itemServerSync = false;

    public static boolean projectileServerSync = false;

    public static boolean fallingBlockServerSync = false;

    public static boolean experienceOrbServerSync = false;

    public static int viewDistanceOverride = -1;

    public static int simulationDistanceOverride = -1;

    public static final int MAX_DISTANCE = 32;

    private static Path configFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("lazytntutils.properties");
    }

    public static void load() {
        Path file = configFile();
        if (!Files.exists(file)) return;
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        } catch (IOException e) {
            LazyTNTUtils.LOGGER.warn("[LazyTNTUtils] 读取服务端配置文件失败，使用默认配置", e);
            return;
        }
        tntServerSync = parse(props, "tntServerSync", tntServerSync);
        itemServerSync = parse(props, "itemServerSync", itemServerSync);
        projectileServerSync = parse(props, "projectileServerSync", projectileServerSync);
        fallingBlockServerSync = parse(props, "fallingBlockServerSync", fallingBlockServerSync);
        experienceOrbServerSync = parse(props, "experienceOrbServerSync", experienceOrbServerSync);
    }

    private static boolean parse(Properties props, String key, boolean fallback) {
        String v = props.getProperty(key);
        return v == null ? fallback : Boolean.parseBoolean(v.trim());
    }

    public static int clampDistance(int value) {
        if (value < -1) return -1;
        return Math.min(value, MAX_DISTANCE);
    }

    public static void resetDistance() {
        viewDistanceOverride = -1;
        simulationDistanceOverride = -1;
    }

    public static void save() {
        Properties props = new Properties();
        props.setProperty("tntServerSync", Boolean.toString(tntServerSync));
        props.setProperty("itemServerSync", Boolean.toString(itemServerSync));
        props.setProperty("projectileServerSync", Boolean.toString(projectileServerSync));
        props.setProperty("fallingBlockServerSync", Boolean.toString(fallingBlockServerSync));
        props.setProperty("experienceOrbServerSync", Boolean.toString(experienceOrbServerSync));
        Path file = configFile();
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream out = Files.newOutputStream(file)) {
                props.store(out, "LazyTNTUtils server sync config");
            }
        } catch (IOException e) {
            LazyTNTUtils.LOGGER.warn("[LazyTNTUtils] 写入服务端配置文件失败", e);
        }
    }
}

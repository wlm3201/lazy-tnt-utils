package com.lazytntutils.client;

public enum MotionArrowMode {
    OFF("off"),

    ARROW("arrow"),

    LINE("line");

    private final String id;

    MotionArrowMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "lazytntutils.config.arrow." + id;
    }

    public static MotionArrowMode fromId(String id) {
        for (MotionArrowMode mode : values()) {
            if (mode.id.equalsIgnoreCase(id)) {
                return mode;
            }
        }
        return ARROW;
    }
}

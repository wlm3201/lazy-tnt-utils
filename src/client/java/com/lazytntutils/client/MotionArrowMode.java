package com.lazytntutils.client;

/**
 * 动量矢量的绘制方式（纯客户端渲染，持久化到 lazytntutils-client.properties）。
 *
 * <p>TNT 与弹射物各持一份（ClientSyncConfig.tntMotionArrow / projectileMotionArrow），按 F3+B 开启碰撞箱时绘制（见
 * EntityHitboxDebugRendererMixin）。原版自己的速度矢量是「线段 + 箭头」， 生电在观察长距离高速抛 TNT /
 * 箭时，箭头那一小撮尖端会盖住弹道末端的位置判断，因此多给一个只画线段的选项。
 */
public enum MotionArrowMode {

  /** 不绘制。 */
  OFF("off", "关闭"),

  /** 线段 + 箭头：与原版速度矢量一致的样式（默认）。 */
  ARROW("arrow", "线段+箭头"),

  /** 只画线段：保留方向与长度，去掉箭头头部。 */
  LINE("line", "仅线段");

  private final String id;

  private final String label;

  MotionArrowMode(String id, String label) {
    this.id = id;
    this.label = label;
  }

  /** 命令与配置文件里使用的标识符。 */
  public String id() {
    return id;
  }

  /** 查询时显示的中文名。 */
  public String label() {
    return label;
  }

  /** 从配置文件读取的字符串还原；空值或未知值回退到 {@link #ARROW}。 */
  public static MotionArrowMode fromId(String id) {
    for (MotionArrowMode mode : values()) {
      if (mode.id.equalsIgnoreCase(id)) {
        return mode;
      }
    }
    return ARROW;
  }
}

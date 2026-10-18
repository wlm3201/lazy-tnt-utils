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
  OFF("off"),

  /** 线段 + 箭头：与原版速度矢量一致的样式（默认）。 */
  ARROW("arrow"),

  /** 只画线段：保留方向与长度，去掉箭头头部。 */
  LINE("line");

  private final String id;

  MotionArrowMode(String id) {
    this.id = id;
  }

  /** 配置文件里使用的标识符（持久化时写入 {@code tntMotionArrow} / {@code projectileMotionArrow}）。 */
  public String id() {
    return id;
  }

  /**
   * 界面显示名的**翻译键**（{@code assets/lazytntutils/lang/*.json} 的 {@code lazytntutils.config.arrow.<id>}）。
   *
   * <p>刻意与 {@link #id()} 分开：持久化写 id（不随语言变化，换语言不会读不回上次的选择），显示走翻译键。
   */
  public String translationKey() {
    return "lazytntutils.config.arrow." + id;
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

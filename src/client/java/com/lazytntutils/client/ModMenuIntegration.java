package com.lazytntutils.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * 在 ModMenu 的模组列表里给本 mod 挂一个"配置"按钮，点开即 {@link ConfigScreen}。
 *
 * <p>这是本 mod 配置界面的唯一入口（原 {@code /lazytntutils} 命令已移除）。
 *
 * <p>## 为什么 ModMenu 可以是可选依赖
 *
 * <p>ModMenu 通过自定义入口点 {@code modmenu} 发现本类：Fabric 只会在"有人按这个 key 取入口点"时才去加载， 而只有装了
 * ModMenu 才会有这一步。因此没装 ModMenu 时本类从头到尾不会被加载，也不会因为缺类而崩 —— 表现为"模组列表里没有配置按钮"，其余功能照常。
 *
 * <p>代价是没装 ModMenu 就没有入口能打开界面（只能手改 config/lazytntutils-client.properties）。 这也是 fabric.mod.json 里把
 * modmenu 声明为 suggests 而非 depends 的原因：depends 会让缺 ModMenu 时整个 mod 拒绝加载， 比"少一个按钮"严重得多。
 */
public class ModMenuIntegration implements ModMenuApi {

  @Override
  public ConfigScreenFactory<?> getModConfigScreenFactory() {
    return ConfigScreen::create;
  }
}

package com.lazytntutils.client;

/**
 * 只需要"位置 + 速度"的实体权威状态（弹射物 / 下落方块共用）。
 *
 * <p>这两类实体在弱加载区的问题与物品完全同构：客户端自己跑运动、服务端不 tick，于是渲染位置一路跑偏。 修正量因此也一致 —— 只要把 pos / motion
 * 换成权威值即可，其余（弹射物的命中、下落方块的落地成块） 都由服务端通过移除 / 变更实体包驱动，客户端不模拟就不会出错。
 */
public record MotionState(double x, double y, double z, double vx, double vy, double vz) {}

/**
 * 末影回响对外事件。
 *
 * <p>事件都派发在 {@code NeoForge.EVENT_BUS} 上，依赖方用 {@code @SubscribeEvent} 监听即可。
 * 当前包括传送 {@link com.unddefined.enderechoing.api.event.EnderEchoTeleportEvent}、
 * 锚点 {@link com.unddefined.enderechoing.api.event.EnderEchoAnchorEvent}、
 * 路径点 {@link com.unddefined.enderechoing.api.event.EnderEchoWaypointEvent}、
 * 水晶 {@link com.unddefined.enderechoing.api.event.EnderEchoCrystalEvent}、
 * 结构 {@link com.unddefined.enderechoing.api.event.EnderEchoStructureEvent}
 * 仪器 {@link com.unddefined.enderechoing.api.event.EnderEchoDeviceEvent}
 * 珍珠 {@link com.unddefined.enderechoing.api.event.EnderEchoPearlEvent}
 * 与队伍 {@link com.unddefined.enderechoing.api.event.EnderEchoTeamEvent} 八族事件。
 */
package com.unddefined.enderechoing.api.event;

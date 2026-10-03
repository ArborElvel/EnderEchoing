/**
 * 末影回响锚点扩展点：让第三方方块、方块实体或结构也能被识别为末影回响锚点。
 *
 * <p>锚点即玩家数据里的 {@code anchors}，是能被绑定为传送目的地的装置；
 * 路径点（{@code waypoints}）是另一回事，见
 * {@link com.unddefined.enderechoing.api.event.EnderEchoWaypointEvent}。
 *
 * <p>注册入口见 {@link com.unddefined.enderechoing.api.anchor.EnderEchoAnchorRegistry}，
 * 判定器接口见 {@link com.unddefined.enderechoing.api.anchor.EnderEchoAnchorProvider}。
 */
package com.unddefined.enderechoing.api.anchor;

/**
 * 末影回响对外扩展接口包。
 *
 * <p>这里的内容面向依赖 enderechoing 的其他模组，会尽量保持向后兼容；
 * {@code server}、{@code blocks}、{@code items}、{@code client} 等包属于内部实现，
 * 不保证兼容性。当前扩展点见 {@code api.anchor} 与 {@code api.event} 两个子包。
 *
 * <p>术语：<b>末影回响锚点</b>（代码中为 {@code anchors}）指可被绑定为传送目的地的装置；
 * <b>路径点</b>（代码中为 {@code waypoints}）指玩家路径点列表里的一条坐标记录。
 * 两者是不同概念，不要混用命名。
 *
 * <p>其余包（{@code server}、{@code blocks}、{@code items}、{@code network}、{@code client}、
 * {@code compat}、{@code mixin} 等）都标了 {@code @ApiStatus.Internal}，不是对外契约。
 *
 * <p>对外版本的递增规则见 {@code EnderEchoingApi.API_VERSION}：只在接口发生不兼容改动时递增，
 * 新增事件或方法不递增。使用说明见仓库根目录的 {@code API.md}。
 */
package com.unddefined.enderechoing.api;

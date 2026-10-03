/**
 * 客户端侧同步事件。
 *
 * <p>这些事件只在物理客户端派发，派发在 {@code NeoForge.EVENT_BUS} 上。
 * 它们表示「客户端缓存的数据被服务端更新了」，不代表权威状态变化；
 * 服务端事件见 {@code com.unddefined.enderechoing.api.event}。
 */
package com.unddefined.enderechoing.client.api.event;

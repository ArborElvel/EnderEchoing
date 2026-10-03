/**
 * 客户端侧对外 API。
 *
 * <p>这些类只在物理客户端存在，服务端（Dedicated Server）不会加载它们：
 * 依赖方不得在通用代码里直接引用，需要配合 {@code Dist} 判断（例如
 * {@code @EventBusSubscriber(value = Dist.CLIENT)} 或 {@code FMLEnvironment.dist.isClient()}）。
 * 服务端侧的对外接口见 {@code com.unddefined.enderechoing.api}。
 */
package com.unddefined.enderechoing.client.api;

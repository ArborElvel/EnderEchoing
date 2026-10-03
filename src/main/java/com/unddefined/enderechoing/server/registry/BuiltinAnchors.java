package com.unddefined.enderechoing.server.registry;

import com.unddefined.enderechoing.api.anchor.EnderEchoAnchorRegistry;
import com.unddefined.enderechoing.blocks.entity.EnderEchoicResonatorBlockEntity;
import com.unddefined.enderechoing.blocks.entity.WarpPlatformBlockEntity;

/**
 * 内置的末影回响锚点判定器，语义与拆分扩展点之前保持一致：
 * 幽匿谐振器与折跃平台。
 */
public final class BuiltinAnchors {
    private BuiltinAnchors() {
    }

    public static void register() {
        EnderEchoAnchorRegistry.register((level, pos) -> {
            var blockEntity = level.getBlockEntity(pos);
            return blockEntity instanceof EnderEchoicResonatorBlockEntity
                    || blockEntity instanceof WarpPlatformBlockEntity;
        });
    }
}

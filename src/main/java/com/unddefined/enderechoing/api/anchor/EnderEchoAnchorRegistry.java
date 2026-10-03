package com.unddefined.enderechoing.api.anchor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 末影回响锚点判定器注册表。
 *
 * <p>注册必须发生在模组加载完成之前（{@code @Mod} 构造函数或 FMLCommonSetupEvent）。
 * 加载完成后注册表会冻结，之后再注册会抛出 {@link IllegalStateException}。
 *
 * <p>判定为“或”语义：任意一个判定器返回 true，该位置就被视为末影回响锚点。
 */
public final class EnderEchoAnchorRegistry {
    private static final List<EnderEchoAnchorProvider> PROVIDERS = new CopyOnWriteArrayList<>();
    private static volatile boolean frozen;

    private EnderEchoAnchorRegistry() {
    }

    /**
     * 注册一个判定器。
     *
     * @throws IllegalStateException 注册表已冻结时抛出
     */
    public static void register(EnderEchoAnchorProvider provider) {
        Objects.requireNonNull(provider, "provider");
        if (frozen) {
            throw new IllegalStateException(
                    "EnderEchoAnchorRegistry is frozen; register anchors during mod construction or FMLCommonSetupEvent");
        }
        PROVIDERS.add(provider);
    }

    /** 该位置是否被任一已注册判定器认可。 */
    public static boolean isAnchor(Level level, BlockPos pos) {
        for (EnderEchoAnchorProvider provider : PROVIDERS) {
            if (provider.isAnchor(level, pos)) return true;
        }
        return false;
    }

    /**
     * 冻结注册表。由 enderechoing 在模组加载完成时调用，外部模组不应调用。
     */
    public static void freeze() {
        frozen = true;
    }
}

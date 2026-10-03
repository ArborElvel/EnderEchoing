package com.unddefined.enderechoing.api;

import com.unddefined.enderechoing.EnderEchoing;
import com.unddefined.enderechoing.api.anchor.EnderEchoAnchors;
import com.unddefined.enderechoing.api.anchor.EnderEchoAnchorProvider;
import com.unddefined.enderechoing.api.anchor.EnderEchoAnchorRegistry;
import com.unddefined.enderechoing.api.team.EnderEchoTeam;
import com.unddefined.enderechoing.api.team.EnderEchoTeams;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 末影回响对外 API 的统一入口。
 *
 * <p>本类只做转发，具体实现仍在各内部包中；依赖方请只通过本包访问，
 * 直接引用内部包不保证兼容性。
 */
public final class EnderEchoingApi {
    /** mod id，与 mods.toml 中一致。 */
    public static final String MODID = EnderEchoing.MODID;

    /**
     * API 版本号。只在对外接口发生不兼容改动（重命名、改签名、改语义、改存档或网络格式）时递增，
     * 新增事件或方法不递增；
     * 依赖方可在自己构造阶段读取本值做兼容分支。
     */
    public static final int API_VERSION = 1;

    private EnderEchoingApi() {
    }

    /**
     * 注册一个末影回响锚点判定器，必须在模组加载完成前调用
     * （通常在 {@code @Mod} 构造函数或 FMLCommonSetupEvent 中）。
     */
    public static void registerAnchor(EnderEchoAnchorProvider provider) {
        EnderEchoAnchorRegistry.register(provider);
    }

    /** 该位置是否被任一已注册判定器认可为末影回响锚点。 */
    public static boolean isAnchor(Level level, BlockPos pos) {
        return EnderEchoAnchors.isAnchor(level, pos);
    }

    /** 玩家所在队伍的只读快照，没有队伍时返回 {@code null}。 */
    @Nullable
    public static EnderEchoTeam teamOf(MinecraftServer server, UUID playerId) {
        return EnderEchoTeams.teamOf(server, playerId);
    }
}

package com.unddefined.enderechoing;

import net.minecraft.util.FastColor;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 客户端外观配置：渲染用的颜色不必改代码就能换。
 *
 * <p>颜色写成 ARGB 十六进制（例如 {@code 0xFF8CF4E2}）。这些值只在客户端读取，
 * 服务端不会加载本配置。贴图与模型仍然走资源包覆盖，见 {@code API.md}。
 */
public class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static ModConfigSpec.IntValue color(String path, String comment, int defaultValue) {
        return BUILDER.comment(comment, "ARGB 十六进制，例如 0xFF8CF4E2")
                .defineInRange(path, defaultValue, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public static final ModConfigSpec.IntValue ECHO_WAVE_COLOR = color(
            "echo_wave_color",
            "回响波纹的普通颜色（未悬停时）。",
            0xFF2CCDB1);

    public static final ModConfigSpec.IntValue ECHO_WAVE_HIGHLIGHT_COLOR = color(
            "echo_wave_highlight_color",
            "回响波纹在悬停/高亮时的颜色。",
            0xFF8CF4E2);

    public static final ModConfigSpec.IntValue ECHO_RESPONSE_COLOR = color(
            "echo_response_color",
            "回响响应波纹（仪器被踩响时那圈波纹）的颜色。",
            0xFF8CF4E2);

    public static final ModConfigSpec.IntValue WAYPOINT_NAME_COLOR = color(
            "waypoint_name_color",
            "世界内路径点 / 谐振器名称文字的颜色。",
            0xFF8CF4E2);

    public static final ModConfigSpec.IntValue WARP_CORE_OUTER_OUTLINE_COLOR = color(
            "warp_core_outer_outline_color",
            "折跃核心外层描边颜色，同时用于传送门星云的染色。",
            0xFF5A2A4D);

    public static final ModConfigSpec.IntValue WARP_CORE_INNER_OUTLINE_COLOR = color(
            "warp_core_inner_outline_color",
            "折跃核心内层描边颜色。",
            0xFF750AED);

    static final ModConfigSpec SPEC = BUILDER.build();

    /**
     * 把配置里的 ARGB 颜色转成顶点色用的 ABGR，并套上给定透明度。
     *
     * @param color 配置项
     * @param alpha 透明度 0-255
     */
    public static int abgr(ModConfigSpec.IntValue color, int alpha) {
        int argb = color.get();
        return FastColor.ABGR32.color(alpha,
                FastColor.ARGB32.red(argb), FastColor.ARGB32.green(argb), FastColor.ARGB32.blue(argb));
    }
}

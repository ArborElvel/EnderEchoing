#version 150

//
// 原版 assets/minecraft/shaders/core/rendertype_end_portal.fsh 的移植。
// 星云坐标 texProj0 来自 gl_Position（见 vsh 的 projectionFromPosition），
// 经 textureProj 的透视除法后就是"屏幕坐标"——图案贴在屏幕上：
// core 怎么转都只是表面从固定的星云上滑过（原版那种窗户感），
// 也不会有任何斜投影剪切或旋转拉伸。
//

uniform sampler2D Sampler0;   // end_sky.png
uniform sampler2D Sampler1;   // end_portal.png

uniform float GameTime;
uniform int EndPortalLayers;

/**
 * 染色：把原版的 16 组颜色往这个颜色混（由 Java 传 WarpCoreRenderer.OUTER_OUTLINE_COLOR）。
 * TINT_MIX = 0 是纯原版观感，1 则完全变成描边色。
 */
uniform vec3 TintColor;
const float TINT_MIX = 0.45;

in vec4 texProj0;

out vec4 fragColor;

// 原样来自 rendertype_end_portal.fsh
const vec3 COLORS[16] = vec3[16](
    vec3(0.022087, 0.098399, 0.110818),
    vec3(0.011892, 0.095924, 0.089485),
    vec3(0.027636, 0.101689, 0.100326),
    vec3(0.046564, 0.109883, 0.114838),
    vec3(0.064901, 0.117696, 0.097189),
    vec3(0.063761, 0.086895, 0.123646),
    vec3(0.084817, 0.111994, 0.166380),
    vec3(0.097489, 0.154120, 0.091064),
    vec3(0.106152, 0.131144, 0.195191),
    vec3(0.097721, 0.110188, 0.187229),
    vec3(0.133516, 0.138278, 0.148582),
    vec3(0.070006, 0.243332, 0.235792),
    vec3(0.196766, 0.142899, 0.214696),
    vec3(0.047281, 0.315338, 0.321970),
    vec3(0.204675, 0.390010, 0.302066),
    vec3(0.080955, 0.314821, 0.661491)
);

const mat4 SCALE_TRANSLATE = mat4(
    0.5, 0.0, 0.0, 0.25,
    0.0, 0.5, 0.0, 0.25,
    0.0, 0.0, 1.0, 0.0,
    0.0, 0.0, 0.0, 1.0
);

mat4 endPortalLayer(float layer) {
    mat4 translate = mat4(
        1.0, 0.0, 0.0, 17.0 / layer,
        0.0, 1.0, 0.0, (2.0 + layer / 1.5) * (GameTime * 1.5),
        0.0, 0.0, 1.0, 0.0,
        0.0, 0.0, 0.0, 1.0
    );

    float angle = radians((layer * layer * 4321.0 + layer * 9.0) * 2.0);
    mat2 rotate = mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
    mat2 scale = mat2((4.5 - layer / 4.0) * 2.0);

    return mat4(scale * rotate) * translate * SCALE_TRANSLATE;
}

/**
 * 附加光晕：同一屏幕投影坐标取更粗的 mip，叠一层柔光。
 * 想要纯净的原版观感就把 GLOW_STRENGTH 设为 0。
 */
const float GLOW_LOD = 3.0;
const float GLOW_SCALE = 1.0;
const float GLOW_STRENGTH = 0.2;

void main() {
    vec3 color = textureProj(Sampler0, texProj0).rgb * COLORS[0];
    for (int i = 0; i < EndPortalLayers; i++) {
        vec3 MIX = vec3(0.0);
        if(i != 0) MIX = mix(COLORS[i], TintColor, TINT_MIX);
        color += textureProj(Sampler1, texProj0 * endPortalLayer(float(i + 1))).rgb * MIX;
    }

    vec3 glow = textureProjLod(Sampler1, texProj0, GLOW_LOD).rgb * GLOW_SCALE;
    fragColor = vec4(color + glow * GLOW_STRENGTH, 1.0);
}

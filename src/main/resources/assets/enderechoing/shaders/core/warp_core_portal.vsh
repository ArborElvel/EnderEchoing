#version 150

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 texProj0;

// 来自原版 shaders/include/projection.glsl
vec4 projectionFromPosition(vec4 position) {
    vec4 projection = position * 0.5;
    projection.xy = vec2(projection.x + projection.w, projection.y + projection.w);
    projection.zw = position.zw;
    return projection;
}

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    // 屏幕空间投影坐标（textureProj 会做透视除法 → NDC*0.5+0.5），
    // 图案因此锚在屏幕上：core 旋转时是表面从星云上滑过，不随表面变形。
    texProj0 = projectionFromPosition(gl_Position);
}

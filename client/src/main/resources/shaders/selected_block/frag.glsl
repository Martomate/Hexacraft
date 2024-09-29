flat in vec3 fragPos;
flat in vec3 fragColor;

layout (location = 0) out vec3 position;
layout (location = 1) out vec3 normal;
layout (location = 2) out vec4 color;

void main() {
    position = fragPos;
    normal = vec3(0);
    color = vec4(fragColor, 1);
}

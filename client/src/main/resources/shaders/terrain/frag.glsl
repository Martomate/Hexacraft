flat in vec3 blockPosition;
flat in vec3 blockColor;
flat in vec3 fragNormal;

layout (location = 0) out vec3 position;
layout (location = 1) out vec3 normal;
layout (location = 2) out vec4 color;

void main() {
	position = blockPosition;
	normal = fragNormal;
	color = vec4(blockColor, 1);
}

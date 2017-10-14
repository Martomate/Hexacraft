#define PI 3.141592653589793
#define y60 0.866025403784439

// Per vertex
in vec3 position;

// Per instance
in ivec3 blockPos;
in vec3 color;

flat out vec3 fragColor;

uniform mat4 projMatrix;
uniform mat4 viewMatrix;
uniform float totalSize;
uniform vec3 cam;

void main() {
	float angleHalfHexagon = PI / totalSize * 2;
	float radius = y60 / angleHalfHexagon;
	
	mat4 matrix = projMatrix * viewMatrix;
	
	vec3 pos = vec3(blockPos.x * 0.75 + position.x, blockPos.y * 0.5 + position.y, (blockPos.x * 0.5 + blockPos.z) + position.z / y60);
	pos.z = mod(mod(pos.z + totalSize / 2, totalSize) - cam.z / y60, totalSize) - totalSize / 2;
	float mult = exp((pos.y - cam.y) / radius);
	float v = pos.z * angleHalfHexagon;
	float z = sin(v);
	float y = cos(v);
	
	float scale = radius / sqrt(z*z+y*y);
	y *= scale;
	z *= scale;
	pos = vec3(pos.x - cam.x, y, z) * mult;
	pos.y -= radius;
	gl_Position = matrix * vec4(pos, 1);
	fragColor = color;
}
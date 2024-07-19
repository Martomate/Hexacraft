#define PI 3.141592653589793
#define y60 0.866025403784439

in ivec3 position;
in vec3 color;

out vec3 blockColor;

uniform mat4 projMatrix;
uniform mat4 viewMatrix;
uniform int totalSize;
uniform vec3 cam;

void main() {
    float totalSizef = float(totalSize);
    float angleHalfHexagon = 2.0 * PI / totalSizef;
    float radius = y60 / angleHalfHexagon;

    mat4 matrix = projMatrix * viewMatrix;

    vec3 pos = vec3(position.x * 0.5, position.y / 32.0 / 6.0, position.z) / 2;
    pos.z -= cam.z / y60;
    float mult = exp((pos.y - cam.y) / radius);
    float v = pos.z * angleHalfHexagon;
    float z = sin(v);
    float y = cos(v);

    y *= radius;
    z *= radius;
    pos = vec3(pos.x - cam.x, y, z) * mult;
    pos.y -= radius;
    gl_Position = matrix * vec4(pos, 1);

    blockColor = color;
}
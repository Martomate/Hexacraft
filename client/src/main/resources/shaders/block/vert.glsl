#define PI 3.141592653589793
#define y60 0.866025403784439

in ivec3 position;
in int texIndex;
in vec3 normal;
in float brightness;
in vec2 texCoords;

struct FragInFlat {
    int texIndex;
};

struct FragIn {
    vec3 position;
    vec2 texCoords;
    float mult;
    float brightness;
    vec3 normal;
};

flat out FragInFlat fragInFlat;
out FragIn fragIn;

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

    fragIn.normal = vec3(normal.x, mat2(y, z, -z, y) * normal.yz);

    y *= radius;
    z *= radius;
    pos = vec3(pos.x - cam.x, y, z) * mult;
    pos.y -= radius;
    gl_Position = matrix * vec4(pos, 1);

    fragIn.position = pos;
    // Here 'mult' is used for correct texturing since block sides are not square
    fragIn.texCoords = vec2(texCoords.x, texCoords.y) * mult;

    fragIn.mult = mult;
    fragInFlat.texIndex = texIndex;
    fragIn.brightness = brightness * brightness;
}
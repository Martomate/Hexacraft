#define PI 3.141592653589793
#define y60 0.866025403784439

// Per vertex
in vec3 position;
in vec2 texCoords;
in vec3 normal;
in int vertexIndex;
in int faceIndex;

// Per instance
in mat4 modelMatrix;
in ivec2 texOffset;
in ivec2 texDim;
in int blockTex;
in float brightness;

struct FragInFlat {
    ivec2 texOffset;
    ivec2 texDim;
    int blockTex;
    int faceIndex;
    float brightness;
};

struct FragIn {
    vec2 texCoords;
    vec3 normal;
};

flat out FragInFlat fragInFlat;
out FragIn fragIn;

uniform mat4 projMatrix;
uniform mat4 viewMatrix;
uniform int totalSize;
uniform int texSize = 32;
uniform vec3 cam;

void main() {
    float totalSizef = float(totalSize);
    float hexAngle = 2.0 * PI / totalSizef;
    float radius = y60 / hexAngle;

    mat4 matrix = projMatrix * viewMatrix;

    vec4 trPos = modelMatrix * vec4(position.x, position.y, position.z, 1);
    trPos.z /= y60;
    vec3 pos = trPos.xyz;//vec3(blockPos.x * 1.5 + position.x, blockPos.y + position.y * blockHeight, (blockPos.x + 2 * blockPos.z) + position.z / y60) / 2;
    pos.z -= cam.z / y60;
    float mult = exp((pos.y - cam.y) / radius);
    float v = pos.z * hexAngle;
    float z = sin(v);
    float y = cos(v);

    fragIn.normal = (modelMatrix * vec4(normal.x, mat2(y, z, -z, y) * normal.yz, 0)).xyz;
    fragIn.normal /= length(fragIn.normal);

    y *= radius;
    z *= radius;
    pos = vec3(pos.x - cam.x, y, z) * mult;
    pos.y -= radius;
    gl_Position = matrix * vec4(pos, 1);
    fragIn.texCoords = texCoords;
    //	fragIn.texCoords = (vec2(texCoords.x, texCoords.y) + texOffset / texSize) * texDim / texSize;
    fragInFlat.texOffset = texOffset;
    fragInFlat.texDim = texDim;
    fragInFlat.blockTex = blockTex;
    fragInFlat.faceIndex = faceIndex;
    fragInFlat.brightness = brightness;
}
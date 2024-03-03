#define PI 3.141592653589793
#define y60 0.866025403784439

// Per vertex
in vec3 position;
in vec2 texCoords;
in vec3 normal;
in int vertexIndex;
in int faceIndex;

// Per instance
in vec2 blockPos;
in int blockTex;
in float blockHeight;
in float brightness;

struct FragInFlat {
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
uniform mat4 viewMatrix = mat4(1);
uniform float windowAspectRatio;

void main() {
    float aspect = windowAspectRatio;
    mat4 matrix = projMatrix * viewMatrix;

    vec3 pos = vec3(position.x, position.y * blockHeight, position.z) / 2;

    fragIn.normal = normal;

    gl_Position = matrix * vec4(pos, 1);
    gl_Position.xy += vec2(blockPos.x / aspect, blockPos.y) * gl_Position.w;
    gl_Position.z = 0;
    fragIn.texCoords = texCoords;
    fragInFlat.blockTex = blockTex;
    fragInFlat.faceIndex = faceIndex;
    fragInFlat.brightness = brightness;
}
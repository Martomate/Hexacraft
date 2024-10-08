#define y60 0.866025403784439

struct FragInFlat {
    ivec2 texOffset;
    ivec2 texDim;
    int blockTex;
    int faceIndex;
    float brightness;
};

struct FragIn {
    vec3 position;
    vec2 texCoords;
    vec3 normal;
};

flat in FragInFlat fragInFlat;
in FragIn fragIn;

layout (location = 0) out vec3 position;
layout (location = 1) out vec3 normal;
layout (location = 2) out vec4 color;

uniform sampler2D texSampler;
uniform int side;
uniform int texSize;

void main() {
    vec2 texCoords = fragIn.texCoords;
    #if isSide
    vec2 coords = texCoords * fragInFlat.texDim;
    int texX = min(int(coords.x), fragInFlat.texDim.x - 1);
    int texY = min(int(coords.y), fragInFlat.texDim.y - 1);
    color = texelFetch(texSampler, ivec2(texX, texY) + fragInFlat.texOffset, 0);
    #else
    float yy = 1 - texCoords.y;
    float xx = texCoords.x + texCoords.y * 0.5;
    vec3 cc = vec3(xx, yy, 2 - xx - yy); // this is 1 - barycentric coords

    int ss = fragInFlat.faceIndex;

    int texDim = fragInFlat.texDim.x;
    float factor = cc.y;
    int xInt = int(cc.x * texDim);
    int zInt = int(cc.z * texDim);
    float px = (xInt-zInt) / factor / texDim;
    int texOffset = (fragInFlat.blockTex >> (4 * (5 - ss)) & 0xffff) >> 12 & 15; // blockTex: 11112222333344445555 + 12 bits
    vec2 tex = vec2(min(1 + px, 1), min(1 - px, 1)) * factor * texDim;
    int texX = min(int(tex.x), texDim - 1);
    int texY = min(int(tex.y), texDim - 1);
    color = texelFetch(texSampler, ivec2(texX + texOffset * texDim, texY) + fragInFlat.texOffset, 0);
    #endif

    position = fragIn.position;
    normal = fragIn.normal;
    color.rgb *= fragInFlat.brightness * 0.8 + 0.2;
}

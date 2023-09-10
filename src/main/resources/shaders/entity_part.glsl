#pragma shader vert
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

#pragma shader frag
#define y60 0.866025403784439

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

flat in FragInFlat fragInFlat;
in FragIn fragIn;

out vec4 color;

uniform sampler2D texSampler;
uniform int side;
uniform int texSize;
uniform vec3 sun;

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

	vec3 sunDir = normalize(sun);
	float visibility = clamp(dot(fragIn.normal, sunDir) * 0.4, 0.0, 0.3) + 0.7;// * (max(sunDir.y * 0.8, 0.0) + 0.2);

	color.rgb *= (fragInFlat.brightness * 0.8 + 0.2) * visibility;
}

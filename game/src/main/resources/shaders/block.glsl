#pragma shader vert
#define PI 3.141592653589793
#define y60 0.866025403784439

#if isSide
const int NUM_VERTICES = 4;
#else
const int NUM_VERTICES = 7;
#endif

// Per vertex
in ivec3 position;
in vec2 texCoords;
in int vertexIndex;
in int faceIndex; // for top and bottom (0 to 5)

// Per instance
in vec3 normal;
in ivec3 blockPos;
in int blockTex;
in vec2 vertexData[NUM_VERTICES]; // blockHeight, brightness

struct FragInFlat {
	int blockTex;
	int faceIndex;
};

struct FragIn {
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

	vec2 vertexData = vertexData[vertexIndex];
	float blockHeight = vertexData.x;
	float brightness = vertexData.y;

	vec3 pos = vec3((blockPos.x + position.x) * 0.5, blockPos.y + position.y * blockHeight, blockPos.z + position.z) / 2;
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

	// Here 'mult' is used for correct texturing since block sides are not square
	fragIn.texCoords = vec2(texCoords.x, texCoords.y) * mult;

#if isSide
	fragIn.texCoords.y *= blockHeight;// TODO: The blockHeight has to use exp() or something...
#endif
	fragIn.mult = mult;
	fragInFlat.blockTex = blockTex;
	fragInFlat.faceIndex = faceIndex;
	fragIn.brightness = brightness;
}

#pragma shader frag
#define y60 0.866025403784439

struct FragInFlat {
	int blockTex;
	int faceIndex;
};

struct FragIn {
	vec2 texCoords;
	float mult;
	float brightness;
	vec3 normal;
};

flat in FragInFlat fragInFlat;
in FragIn fragIn;

out vec4 color;

uniform sampler2DArray texSampler;
uniform int side;
uniform int texSize = 32;
uniform vec3 sun;

void main() {
	float texSizef = float(texSize);
	int texDepth = fragInFlat.blockTex & 0xfff;
	vec2 texCoords = vec2(fragIn.texCoords.x / fragIn.mult, fragIn.texCoords.y / fragIn.mult);

#if isSide
	color = texture(texSampler, vec3(texCoords, texDepth));
#else
	float yy = 1 - texCoords.y;
	float xx = texCoords.x + texCoords.y * 0.5;
	vec3 cc = vec3(xx, yy, 2 - xx - yy); // this is 1 - barycentric coords

	int ss = fragInFlat.faceIndex;

	float factor = cc.y;
	int xInt = int(cc.x*texSizef);
	int zInt = int(cc.z*texSizef);
	float px = (xInt-zInt) / factor / texSizef;

	int texOffset = (fragInFlat.blockTex >> (4 * (5 - ss)) & 0xffff) >> 12 & 15; // blockTex: 11112222333344445555 + 12 bits

	vec2 tex = vec2(min(1 + px, 1), min(1 - px, 1)) * factor * texSizef;
	int texX = clamp(int(tex.x), 0, texSize - 1);
	int texY = clamp(int(tex.y), 0, texSize - 1);
	ivec2 stCoords = ivec2(int(tex.x), int(tex.y));
	color = textureGrad(
		texSampler,
		vec3(vec2(stCoords) / texSizef, texDepth + texOffset),
		dFdx(fragIn.texCoords / fragIn.mult),
		dFdy(fragIn.texCoords / fragIn.mult));
#endif

	vec3 sunDir = normalize(sun);
	float visibility = 1 - (side < 2 ? side * 3 : (side - 2) % 2 + 1) * 0.05;//max(min(dot(fragIn.normal, sunDir) * 0.4, 0.3), 0.0) + 0.7;// * (max(sunDir.y * 0.8, 0.0) + 0.2);

	color.rgb *= (fragIn.brightness * 0.8 + 0.2) * visibility;
}

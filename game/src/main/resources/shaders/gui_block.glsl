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

#pragma shader frag
#define y60 0.866025403784439

struct FragInFlat {
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

uniform sampler2DArray texSampler;
uniform int side;
uniform int texSize = 32;

void main() {
	float texSizef = float(texSize);
	int texDepth = fragInFlat.blockTex & 0xfff;
	vec2 texCoords = fragIn.texCoords;

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
		dFdx(fragIn.texCoords),
		dFdy(fragIn.texCoords));
#endif

	float visibility = 1 - (side < 2 ? side * 3 : (side - 2) % 2 + 1) * 0.05;//max(min(dot(fragIn.normal, sunDir) * 0.4, 0.3), 0.0) + 0.7;// * (max(sunDir.y * 0.8, 0.0) + 0.2);

	color.rgb *= fragInFlat.brightness * visibility;
}

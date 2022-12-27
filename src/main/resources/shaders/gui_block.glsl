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
uniform vec2 windowSize;

void main() {
	float aspect = windowSize.x / windowSize.y;
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

ivec2 triCoordsToStorage(in ivec2 triCoords);

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

	vec2 tex = vec2(1 + px, 1) * factor * texSizef;
	int texX = clamp(int(tex.x), 0, texSize * 2);
	int texY = clamp(int(tex.y), 0, texSize - 1);
	ivec2 stCoords = triCoordsToStorage(ivec2(texX, texY));

	color = textureGrad(
		texSampler,
		vec3(vec2(stCoords) / texSizef, texDepth + texOffset),
		dFdx(fragIn.texCoords),
		dFdy(fragIn.texCoords));
#endif

	float visibility = 1 - (side < 2 ? side * 3 : (side - 2) % 2 + 1) * 0.05;//max(min(dot(fragIn.normal, sunDir) * 0.4, 0.3), 0.0) + 0.7;// * (max(sunDir.y * 0.8, 0.0) + 0.2);

	color.rgb *= fragInFlat.brightness * visibility;
}

// Recursive storage format
ivec2 triCoordsToStorage(in ivec2 triCoords) {
	int sx = 0;
	int sy = 0;
	int bsize = texSize;
	int x = triCoords.x;
	int y = triCoords.y;
	int sign = 1;

	while (bsize > 0) {
		int rest = y - bsize;

		if (y < bsize) {
		} else if (x <= 2 * rest) { // left
			sy += bsize * sign;
			y = rest;
		} else if (x >= 2 * bsize) { // right
			sx += bsize * sign;
			x -= 2 * bsize;
			y = rest;
		} else {
			sx += (2 * bsize - 1) * sign;
			sy += (2 * bsize - 1) * sign;
			sign = -sign;
			x = 2 * bsize - 1 - x;
			y = bsize - 1 - rest;
		}

		bsize >>= 1;
	}

	return ivec2(sx, sy);
}

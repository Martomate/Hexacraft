#shader vert
#define PI 3.141592653589793
#define y60 0.866025403784439

// Per vertex
in vec3 position;
in vec2 texCoords;
in vec3 normal;
in int vertexIndex;

// Per instance
in ivec3 blockPos;
in int blockTex;
in float blockHeight;

#if isSide
in float brightness[4];
#else
in float brightness[6];
#endif

out FragIn {
	vec2 texCoords;
	flat int blockTex;
	float brightness;
	vec3 normal;
} fragIn;

uniform mat4 projMatrix;
uniform mat4 viewMatrix;
uniform float totalSize;
uniform vec3 cam;

void main() {
	float angleHalfHexagon = PI / totalSize * 2;
	float radius = y60 / angleHalfHexagon;

	mat4 matrix = projMatrix * viewMatrix;

	vec3 pos = vec3(blockPos.x * 1.5 + position.x, blockPos.y + position.y * blockHeight, (blockPos.x + 2 * blockPos.z) + position.z / y60) / 2;
	pos.z -= cam.z / y60;
	float mult = exp((pos.y - cam.y) / radius);
	float v = pos.z * angleHalfHexagon;
	float z = sin(v);
	float y = cos(v);

	fragIn.normal = vec3(normal.x, mat2(y, z, -z, y) * normal.yz);

	float scale = radius / sqrt(z*z+y*y);// to fix rounding errors
	y *= scale;
	z *= scale;
	pos = vec3(pos.x - cam.x, y, z) * mult;
	pos.y -= radius;
	gl_Position = matrix * vec4(pos, 1);
	fragIn.texCoords = vec2(texCoords.x, texCoords.y);
	fragIn.blockTex = blockTex;
	fragIn.brightness = brightness[vertexIndex];
}

#shader frag
#define y60 0.866025403784439

in FragIn {
	vec2 texCoords;
	flat int blockTex;
	float brightness;
	vec3 normal;
} fragIn;

out vec4 color;

uniform sampler2DArray texSampler;
uniform int side;
uniform int texSize = 32;
uniform vec3 sun;

void main() {
	int texDepth = fragIn.blockTex & 0xfff;

#if isSide
	color = texture(texSampler, vec3(fragIn.texCoords, texDepth));
#else
	float yy = (fragIn.texCoords.y * 2 - 1) / y60;
	float xx = fragIn.texCoords.x + yy * 0.25;
	yy = (yy + 1) * 0.5;
	float zz = yy - xx + 0.5;
	vec3 pp = vec3(xx, yy, zz) * 2 - 1;
	vec3 cc = 1 - abs(pp);

	int ss, ppp = (pp.x >= 0 ? 1 : 0) << 2 | (pp.y >= 0 ? 1 : 0) << 1 | (pp.z >= 0 ? 1 : 0);

	switch (ppp) {
		case 6: // 110
			cc.x = 1-cc.x;
			ss = 0;
			break;
		case 1: // 001
			cc.x = 1-cc.x;
			ss = 3;
			break;
		case 5: // 101
		case 7: // 111
			cc.y = 1-cc.y;
			ss = 1;
			break;
		case 2: // 010
		case 0: // 000
			cc.y = 1-cc.y;
			ss = 4;
			break;
		case 3: // 011
			cc.z = 1-cc.z;
			ss = 2;
			break;
		case 4: // 100
			cc.z = 1-cc.z;
			ss = 5;
			break;
	}

	float factor = cc.y;
	int xInt = int(cc.x*texSize);
	int zInt = int(cc.z*texSize);
	float px = (xInt-zInt) / factor / texSize;
	vec2 tex = vec2(min(1 + px, 1), min(1 - px, 1)) * factor;
	int texOffset = (fragIn.blockTex >> (4 * (5 - ss)) & 0xfff) >> 12 & 15; // blockTex: 11112222333344445555 + 12 bits
	color = texture(texSampler, vec3(tex, texDepth + texOffset));
#endif

	vec3 sunDir = normalize(sun);
	float visibility = 1 - (side < 2 ? side * 3 : (side - 2) % 2 + 1) * 0.05;//max(min(dot(fragIn.normal, sunDir) * 0.4, 0.3), 0.0) + 0.7;// * (max(sunDir.y * 0.8, 0.0) + 0.2);

	color.rgb *= (fragIn.brightness * 0.8 + 0.2) * visibility;
}
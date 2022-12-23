#pragma shader vert
#define PI 3.141592653589793
#define y60 0.866025403784439

// Per vertex
in vec3 position;
in vec2 texCoords;
in vec3 normal;
in int vertexIndex;
in int ss;

// Per instance
in ivec3 blockPos;
in int blockTex;
in float blockHeight;

#if isSide
in float brightness[4];
#else
in float brightness[7];
#endif

out FragIn {
	vec2 texCoords;
	float mult;
	float brightness;
	vec3 normal;
} fragIn;
flat out int fragBlockTex;
flat out int fragSs;

uniform mat4 projMatrix;
uniform mat4 viewMatrix;
uniform int totalSize;
uniform vec3 cam;

void main() {
	float totalSizef = float(totalSize);
	float angleHalfHexagon = 2.0 * PI / totalSizef;
	float radius = y60 / angleHalfHexagon;

	mat4 matrix = projMatrix * viewMatrix;

	vec3 pos = vec3(blockPos.x * 1.5 + position.x, blockPos.y + position.y * blockHeight, (blockPos.x + 2 * blockPos.z) + position.z / y60) / 2;
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
	fragBlockTex = blockTex;
	fragSs = ss;
	fragIn.brightness = brightness[vertexIndex];
}

#pragma shader frag
#define y60 0.866025403784439

ivec2 triCoordsToStorage(in ivec2 triCoords);

in FragIn {
	vec2 texCoords;
	float mult;
	float brightness;
	vec3 normal;
} fragIn;
flat in int fragBlockTex;
flat in int fragSs;

out vec4 color;

uniform sampler2DArray texSampler;
uniform int side;
uniform int texSize = 32;
uniform vec3 sun;

void main() {
	float texSizef = float(texSize);
	int texDepth = fragBlockTex & 0xfff;
	vec2 texCoords = vec2(fragIn.texCoords.x / fragIn.mult, fragIn.texCoords.y / fragIn.mult);

#if isSide
	color = texture(texSampler, vec3(texCoords, texDepth));
#else
	float yy = (texCoords.y * 2 - 1) / y60;
	float xx = texCoords.x + yy * 0.25;
	yy = (yy + 1) * 0.5;
	float zz = yy - xx + 0.5;
	vec3 pp = vec3(xx, yy, zz) * 2 - 1;
	vec3 cc = 1 - abs(pp);

	int ppp = (pp.x >= 0 ? 1 : 0) << 2 | (pp.y >= 0 ? 1 : 0) << 1 | (pp.z >= 0 ? 1 : 0);
	int ss = fragSs;

	switch (ppp) {
		case 6: // 110
			cc.x = 1-cc.x;
			break;
		case 1: // 001
			cc.x = 1-cc.x;
			break;
		case 5: // 101
		case 7: // 111
			cc.y = 1-cc.y;
			break;
		case 2: // 010
		case 0: // 000
			cc.y = 1-cc.y;
			break;
		case 3: // 011
			cc.z = 1-cc.z;
			break;
		case 4: // 100
			cc.z = 1-cc.z;
			break;
	}

	float factor = cc.y;
	int xInt = int(cc.x*texSizef);
	int zInt = int(cc.z*texSizef);
	float px = (xInt-zInt) / factor / texSizef;

	int texOffset = (fragBlockTex >> (4 * (5 - ss)) & 0xffff) >> 12 & 15; // blockTex: 11112222333344445555 + 12 bits

	vec2 tex = vec2(1 + px, 1) * factor * texSizef;
	int texX = clamp(int(tex.x), 0, texSize * 2);
	int texY = clamp(int(tex.y), 0, texSize - 1);
	ivec2 stCoords = triCoordsToStorage(ivec2(texX, texY));
	color = textureGrad(
		texSampler,
		vec3(vec2(stCoords) / texSizef, texDepth + texOffset),
		dFdx(fragIn.texCoords / fragIn.mult) * 2,
		dFdy(fragIn.texCoords / fragIn.mult) * 2);
#endif

	vec3 sunDir = normalize(sun);
	float visibility = 1 - (side < 2 ? side * 3 : (side - 2) % 2 + 1) * 0.05;//max(min(dot(fragIn.normal, sunDir) * 0.4, 0.3), 0.0) + 0.7;// * (max(sunDir.y * 0.8, 0.0) + 0.2);

	color.rgb *= (fragIn.brightness * 0.8 + 0.2) * visibility;
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

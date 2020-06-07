#shader vert
#define PI 3.141592653589793
#define y60 0.866025403784439

// Per vertex
in vec3 position;
in vec2 texCoords;
in vec3 normal;
in int vertexIndex;

// Per instance
in vec2 blockPos;
in int blockTex;
in float blockHeight;
in float brightness;

out FragIn {
	vec2 texCoords;
	vec3 normal;
} fragIn;
flat out int fragBlockTex;
flat out float fragBrightness;

uniform mat4 projMatrix;
uniform mat4 viewMatrix = mat4(1);
uniform vec2 windowSize;

float aspect = windowSize.x / windowSize.y;

void main() {
	mat4 matrix = projMatrix * viewMatrix;

	vec3 pos = vec3(position.x, position.y * blockHeight, position.z) / 2;

	fragIn.normal = normal;

	gl_Position = matrix * vec4(pos, 1);
	gl_Position.xy += vec2(blockPos.x / aspect, blockPos.y) * gl_Position.w;
	gl_Position.z = 0;
	fragIn.texCoords = texCoords;
	fragBlockTex = blockTex;
	fragBrightness = brightness;
}

#shader frag
#define y60 0.866025403784439

in FragIn {
	vec2 texCoords;
	vec3 normal;
} fragIn;
flat in int fragBlockTex;
flat in float fragBrightness;

out vec4 color;

uniform sampler2DArray texSampler;
uniform int side;
uniform int texSize = 32;

float texSizef = float(texSize);

void main() {
	int texDepth = fragBlockTex & 0xfff;

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
	int xInt = int(cc.x*texSizef);
	int zInt = int(cc.z*texSizef);
	float px = (xInt-zInt) / factor / texSizef;
	vec2 tex = vec2(min(1 + px, 1), min(1 - px, 1)) * factor;
	int texOffset = (fragBlockTex >> (4 * (5 - ss)) & 0xfff) >> 12 & 15; // blockTex: 11112222333344445555 + 12 bits
	color = texture(texSampler, vec3(tex, texDepth + texOffset));
#endif

	float visibility = 1 - (side < 2 ? side * 3 : (side - 2) % 2 + 1) * 0.05;//max(min(dot(fragIn.normal, sunDir) * 0.4, 0.3), 0.0) + 0.7;// * (max(sunDir.y * 0.8, 0.0) + 0.2);

	color.rgb *= fragBrightness * visibility;
}

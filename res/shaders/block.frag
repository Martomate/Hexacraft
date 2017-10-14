#define y60 0.866025403784439

in FragIn {
	vec2 texCoords;
	flat int blockTex;
	vec3 normal;
} fragIn;

out vec4 color;

uniform sampler2DArray texSampler;
uniform float glow = 1.0;
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
	
	color.rgb *= glow * visibility;
}
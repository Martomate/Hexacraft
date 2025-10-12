#define y60 0.866025403784439

struct FragInFlat {
	int texIndex;
};

struct FragIn {
	vec3 position;
	vec2 texCoords;
	float mult;
	float lightCloseness;
	vec3 normal;
};

flat in FragInFlat fragInFlat;
in FragIn fragIn;

layout (location = 0) out vec3 position;
layout (location = 1) out vec3 normal;
layout (location = 2) out vec4 color;

uniform sampler2DArray texSampler;
uniform int side;
uniform int texSize = 32;

void main() {
	float texSizef = float(texSize);
	vec2 texCoords = vec2(fragIn.texCoords.x / fragIn.mult, fragIn.texCoords.y / fragIn.mult);

#if isSide
	color = texture(texSampler, vec3(texCoords, fragInFlat.texIndex));
#else
	float yy = 1 - texCoords.y;
	float xx = texCoords.x + texCoords.y * 0.5;
	vec3 cc = vec3(xx, yy, 2 - xx - yy); // this is 1 - barycentric coords

	float factor = cc.y;
	int xInt = int(cc.x*texSizef);
	int zInt = int(cc.z*texSizef);
	float px = (xInt-zInt) / factor / texSizef;

	vec2 tex = vec2(min(1 + px, 1), min(1 - px, 1)) * factor * texSizef;
	int texX = clamp(int(tex.x), 0, texSize - 1);
	int texY = clamp(int(tex.y), 0, texSize - 1);
	ivec2 stCoords = ivec2(int(tex.x), int(tex.y));
	color = textureGrad(
		texSampler,
		vec3(vec2(stCoords) / texSizef, fragInFlat.texIndex),
		dFdx(fragIn.texCoords / fragIn.mult),
		dFdy(fragIn.texCoords / fragIn.mult));
#endif

	position = fragIn.position;
	normal = fragIn.normal;
    // lightCloseness = 1 - r, so it goes from 1 to 0 linearly
    // therefore (1-r)^2 is a better fit than d^2/(r+d)^2 which would never reach 0
    float brightness = fragIn.lightCloseness * fragIn.lightCloseness;
    color.rgb *= brightness * 0.8 + 0.2;

    // TODO: the ambient occlusion looks too big now, so maybe interpolate that separately
}

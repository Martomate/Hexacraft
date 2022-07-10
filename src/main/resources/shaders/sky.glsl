#pragma shader vert

in vec2 position;

out vec2 fragPosition;

void main() {
	gl_Position = vec4(position, 0.0, 1.0);
	fragPosition = position;
}

#pragma shader frag

in vec2 fragPosition;

out vec4 color;

uniform mat4 invProjMatr;
uniform mat4 invViewMatr;
uniform vec3 sun;

void main() {
	vec4 coordsBeforeMatr = vec4(fragPosition.x, fragPosition.y, -1, 1);
	vec4 coordsAfterProj = invProjMatr * coordsBeforeMatr;
	vec3 ray = normalize((invViewMatr * vec4(coordsAfterProj.xy, -1, 0)).xyz);
	vec3 theSun = normalize(sun);
	float sunBrightness = min(exp((dot(ray, theSun)-1)*400), 1) * 0.5;
	float sunGlow      = exp((dot(ray, theSun)-1)*4) * 0.4;
	vec3 up = vec3(0, 1, 0);
	float rayUp = dot(ray, up);
	float gradientFalloff = 0.5;
	if (rayUp < 0) gradientFalloff = 2.0;
	// TODO: The world is cylindrical!
	vec3 col = sunBrightness * vec3(0.8, 0.65, 0.8) + sunGlow * vec3(0.8, 0.65, 0.8) + vec3(0.4, 0.7, 0.5) * (1 - abs(rayUp) * gradientFalloff) + vec3(0.0, 0.0, 0.7);
	color = vec4(col, 1);
}
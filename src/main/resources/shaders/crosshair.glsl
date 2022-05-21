#pragma shader vert

in vec2 position;

uniform vec2 windowSize;

float aspect = windowSize.x / windowSize.y;

void main() {
	gl_Position = vec4(position.x, position.y * aspect, 0.0, 1.0);
}

#pragma shader frag

out vec4 color;

uniform vec2 windowSize;

void main() {
//	vec2 pos = gl_FragCoord.xy - windowSize * 0.5;
	color = vec4(1, 1, 1, 1);
}
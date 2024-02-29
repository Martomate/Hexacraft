#pragma shader vert

in vec2 position;

uniform float windowAspectRatio;

void main() {
	gl_Position = vec4(position.x / windowAspectRatio, position.y, 0.0, 1.0);
}

#pragma shader frag

out vec4 color;

void main() {
	color = vec4(1, 1, 1, 1);
}
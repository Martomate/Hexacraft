#pragma shader vert

in vec2 position;

uniform mat4 transformationMatrix;
uniform float windowAspectRatio;

void main() {
	gl_Position = transformationMatrix * vec4(position, 0.0, 1.0);
	gl_Position.xy *= vec2(1 / windowAspectRatio, 1);
}

#pragma shader frag

out vec4 color;

uniform vec4 col;

void main() {
	color = col;
}

in vec2 position;

uniform vec2 windowSize;

float aspect = windowSize.x / windowSize.y;

void main() {
	gl_Position = vec4(position.x, position.y * aspect, 0.0, 1.0);
}
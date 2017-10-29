
in vec2 position;

uniform mat4 transformationMatrix;

void main() {
	gl_Position = transformationMatrix * vec4(position, 0.0, 1.0) * 2 - 1;
}
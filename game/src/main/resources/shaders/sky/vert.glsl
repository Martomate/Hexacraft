in vec2 position;

out vec2 fragPosition;

void main() {
    gl_Position = vec4(position, 0.0, 1.0);
    fragPosition = position;
}
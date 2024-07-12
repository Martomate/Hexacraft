in vec2 position;

uniform float windowAspectRatio;

void main() {
    gl_Position = vec4(position.x / windowAspectRatio, position.y, 0.0, 1.0);
}
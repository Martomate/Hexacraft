in vec2 position;

out vec2 textureCoords;

uniform mat4 transformationMatrix;
uniform float windowAspectRatio;

void main() {
    gl_Position = transformationMatrix * vec4(position, 0.0, 1.0);
    gl_Position.xy *= vec2(1 / windowAspectRatio, 1);
    textureCoords = vec2(position.x, 1 - position.y);
}
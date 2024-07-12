in vec2 position;

out vec2 internalPosition;
out float internalAspectRatio;

uniform mat4 transformationMatrix;
uniform float windowAspectRatio;

void main() {
    internalPosition = position;
    vec4 internalAspectVec = transformationMatrix * vec4(1, 1, 0, 0);
    internalAspectRatio = internalAspectVec.y / internalAspectVec.x;

    gl_Position = transformationMatrix * vec4(position, 0.0, 1.0);
    gl_Position.xy *= vec2(1 / windowAspectRatio, 1);
}
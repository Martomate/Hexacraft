in vec2 internalPosition;
in float internalAspectRatio;

out vec4 color;

uniform vec4 col;
uniform bool inverted;

void main() {
    vec2 distToEdge2d = 0.5 - abs(internalPosition - 0.5);
    float distToEdge = min(distToEdge2d.x / internalAspectRatio, distToEdge2d.y);
    bool isCloseToEdge = distToEdge < 0.05;
    float edgeEffect = smoothstep(0, 1, 1 - distToEdge / 0.05);

    float topLeftNess = 0;
    if (internalPosition.y < 0.5) {
        topLeftNess = (clamp((internalPosition.y * internalAspectRatio - internalPosition.x) * 200, -1, 1) + 1) * 0.5;
    } else {
        topLeftNess = (clamp(((1 - internalPosition.x) - (1 - internalPosition.y) * internalAspectRatio) * 200, -1, 1) + 1) * 0.5;
    }

    if (inverted) topLeftNess = 1 - topLeftNess;

    if (isCloseToEdge) {
        color = col + vec4(1, 1, 1, 0) * edgeEffect * mix(-0.3, 0.3, smoothstep(0, 1, topLeftNess));
    } else {
        color = col;
    }
}
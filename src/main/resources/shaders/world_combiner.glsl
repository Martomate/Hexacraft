#pragma shader vert

in vec2 position;

out vec2 textureCoords;

void main() {
    gl_Position = vec4(position, 0.0, 1.0);
    textureCoords = position * 0.5 + 0.5;
}

#pragma shader frag

in vec2 textureCoords;

out vec4 color;

uniform sampler2D worldColorTexture;
uniform sampler2D worldDepthTexture;
uniform float nearPlane;
uniform float farPlane;

float linearize_depth(float d,float zNear,float zFar)
{
    return zNear * zFar / (zFar + d * (zNear - zFar));
}

void main() {
    vec4 worldColor = texture(worldColorTexture, textureCoords);
    float worldDepth = linearize_depth(texture(worldDepthTexture, textureCoords).r, nearPlane, farPlane);
    color = worldColor;//vec4(mix(worldColor.rgb, vec3(0.0, 0.0, 0.5), 1.0 - exp(-0.2 * worldDepth)), 1.0);
}

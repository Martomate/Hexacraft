in vec2 textureCoords;

out vec4 color;

uniform sampler2D worldPositionTexture;
uniform sampler2D worldNormalTexture;
uniform sampler2D worldColorTexture;
uniform sampler2D worldDepthTexture;
uniform float nearPlane;
uniform float farPlane;
uniform vec3 sun;

float linearize_depth(float d,float zNear,float zFar)
{
    return zNear * zFar / (zFar + d * (zNear - zFar));
}

void main() {
    vec3 worldPosition = texture(worldPositionTexture, textureCoords).rgb;
    vec3 worldNormal = texture(worldNormalTexture, textureCoords).rgb;
    vec4 worldColor = texture(worldColorTexture, textureCoords);
    float worldDepth = linearize_depth(texture(worldDepthTexture, textureCoords).r, nearPlane, farPlane);

    vec3 sunDir = normalize(sun);
    float visibility = max(dot(worldNormal, sunDir), 0) * 0.2 + 0.8;

    color = worldColor;//vec4(mix(worldColor.rgb, vec3(0.0, 0.0, 0.5), 1.0 - exp(-0.2 * worldDepth)), 1.0);
    color.rgb *= visibility;
}

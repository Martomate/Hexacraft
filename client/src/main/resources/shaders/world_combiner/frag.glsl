#define PI 3.141592653589793
#define y60 0.866025403784439

in vec2 textureCoords;

out vec4 color;

uniform sampler2D worldPositionTexture;
uniform sampler2D worldNormalTexture;
uniform sampler2D worldColorTexture;
uniform sampler2D worldDepthTexture;
uniform float nearPlane;
uniform float farPlane;
uniform vec3 sun;
uniform int totalSize;

uniform vec3 waterFogColor;
uniform vec3 waterAbsorption;
uniform float waterScattering;
uniform float waterDepthDarkening;
uniform float waterSurfaceAboveEye; // height of the water surface relative to the eye (in CylCoords)
uniform float waterFogStrength; // 0 when there is no water around the eye, 1 when the eye is under water

float linearize_depth(float d,float zNear,float zFar)
{
    return zNear * zFar / (zFar + d * (zNear - zFar));
}

// Returns how far above the eye the given point is, measured in world space (i.e. before the world was curved).
// This undoes the curvature applied in the vertex shaders, where pos.y = R * exp(h / R) * cos(v) - R and
// pos.z = R * exp(h / R) * sin(v), so (pos.y + R)^2 + pos.z^2 = R^2 * exp(2h / R).
float heightAboveEye(vec3 pos) {
    float radius = y60 / (2.0 * PI / float(totalSize));
    // e = exp(2h / R) - 1, computed without subtracting large numbers
    float e = (2.0 * pos.y + (pos.y * pos.y + pos.z * pos.z) / radius) / radius;
    // log(1 + e) loses precision for small e, so use the Taylor expansion there
    float logOnePlusE = abs(e) < 1e-3 ? e * (1.0 - e * (0.5 - e / 3.0)) : log(1.0 + e);
    return 0.5 * radius * logOnePlusE;
}

// Returns how much of the straight line from the eye to a point (at distance dist and height h) lies below the water
float underwaterDistance(float dist, float h) {
    float surface = waterSurfaceAboveEye;

    // The line goes from height 0 to height h, and the part below the surface is under water
    if (abs(h) < 1e-5) return surface > 0.0 ? dist : 0.0;
    float t = clamp(surface / h, 0.0, 1.0);
    return h > 0.0 ? dist * t : dist * (1.0 - t);
}

// Returns how much of the sunlight is left after travelling down to the given depth below the surface
vec3 lightAtDepth(float depth) {
    return exp(-(waterAbsorption + waterDepthDarkening) * max(depth, 0.0));
}

// The light from the object is absorbed (red first), and is replaced by light scattered by the water itself
vec3 applyWaterFog(vec3 col, vec3 fogColor, float underwaterDist) {
    vec3 transmittance = exp(-waterAbsorption * underwaterDist);
    float scattered = 1.0 - exp(-waterScattering * underwaterDist);
    return mix(col * transmittance, fogColor, scattered);
}

void main() {
    vec3 worldPosition = texture(worldPositionTexture, textureCoords).rgb;
    vec3 worldNormal = texture(worldNormalTexture, textureCoords).rgb;
    vec4 worldColor = texture(worldColorTexture, textureCoords);
    float worldDepth = linearize_depth(texture(worldDepthTexture, textureCoords).r, nearPlane, farPlane);

    vec3 sunDir = normalize(sun);
    float visibility = max(dot(worldNormal, sunDir), 0) * 0.2 + 0.8;

    color = worldColor;
    color.rgb *= visibility;
    color.a = sqrt(color.a);
    // The color was premultiplied when blended into the (transparent) frame buffer, so undo that before blending again
    if (color.a > 0.0) color.rgb /= color.a;

    if (waterFogStrength > 0.0 && color.a > 0.0) {
        // The position was also blended into the frame buffer, so it has to be un-premultiplied as well
        vec3 pos = worldPosition / color.a;
        float h = heightAboveEye(pos);
        float s = waterFogStrength;

        // Deep down there is less sunlight, both on the objects and in the water between them and the eye
        vec3 objectColor = color.rgb * lightAtDepth((waterSurfaceAboveEye - h) * s);
        vec3 fogColor = waterFogColor * lightAtDepth(waterSurfaceAboveEye * s);

        color.rgb = applyWaterFog(objectColor, fogColor, underwaterDistance(length(pos), h) * s);
    }
}

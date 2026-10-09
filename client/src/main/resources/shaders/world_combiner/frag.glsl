#define PI 3.141592653589793
#define y60 0.866025403784439

in vec2 textureCoords;

out vec4 color;

uniform sampler2D worldPositionTexture;
uniform sampler2D worldNormalTexture;
uniform sampler2D worldColorTexture;
uniform sampler2D worldDepthTexture;
uniform sampler2D translucentPositionTexture;
uniform sampler2D translucentNormalTexture;
uniform sampler2D translucentColorTexture;
uniform float nearPlane;
uniform float farPlane;
uniform vec3 sun;
uniform int totalSize;

uniform vec3 waterFogColor;
uniform vec3 waterAbsorption;
uniform float waterScattering;
uniform float waterDepthDarkening;
uniform float waterSurfaceAboveEye; // height of the water surface relative to the eye (in CylCoords)
uniform float waterFogStrength; // 1 when the eye is under water, otherwise 0

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

struct Layer {
    vec3 position;
    vec3 normal;
    vec3 color;
    float alpha;
};

// Reads a layer from the G-buffer, and undoes the premultiplication that happened when it was blended into it
Layer readLayer(sampler2D positionTexture, sampler2D normalTexture, sampler2D colorTexture) {
    vec4 col = texture(colorTexture, textureCoords);
    Layer layer;
    layer.alpha = sqrt(col.a);
    float div = layer.alpha > 0.0 ? layer.alpha : 1.0;
    layer.position = texture(positionTexture, textureCoords).rgb / div;
    layer.normal = texture(normalTexture, textureCoords).rgb;
    layer.color = col.rgb / div;

    vec3 sunDir = normalize(sun);
    float visibility = max(dot(layer.normal, sunDir), 0) * 0.2 + 0.8;
    layer.color *= visibility;
    return layer;
}

// Applies the fog of the water that the eye is in (if any) on the light coming from the given position
vec3 applyEyeWaterFog(vec3 col, vec3 pos) {
    if (waterFogStrength <= 0.0) return col;

    float h = heightAboveEye(pos);
    float s = waterFogStrength;

    // Deep down there is less sunlight, both on the objects and in the water between them and the eye
    vec3 objectColor = col * lightAtDepth((waterSurfaceAboveEye - h) * s);
    vec3 fogColor = waterFogColor * lightAtDepth(waterSurfaceAboveEye * s);

    return applyWaterFog(objectColor, fogColor, underwaterDistance(length(pos), h) * s);
}

void main() {
    Layer opaque = readLayer(worldPositionTexture, worldNormalTexture, worldColorTexture);
    Layer translucent = readLayer(translucentPositionTexture, translucentNormalTexture, translucentColorTexture);

    vec3 opaqueColor = opaque.color;
    if (opaque.alpha > 0.0 && translucent.alpha > 0.0) {
        // If the front of the water is seen (e.g. the surface from above) the opaque thing behind it is under water
        bool seenThroughWater = dot(translucent.normal, translucent.position) < 0.0;
        if (seenThroughWater) {
            float dist = max(length(opaque.position) - length(translucent.position), 0.0);
            float depth = heightAboveEye(translucent.position) - heightAboveEye(opaque.position);
            opaqueColor = applyWaterFog(opaqueColor * lightAtDepth(depth), waterFogColor, dist);
        }
    }
    opaqueColor = applyEyeWaterFog(opaqueColor, opaque.position);
    vec3 translucentColor = applyEyeWaterFog(translucent.color, translucent.position);

    // Put the translucent layer on top of the opaque layer, and the result will be put on top of the sky
    float alpha = translucent.alpha + opaque.alpha * (1.0 - translucent.alpha);
    vec3 premultiplied = translucentColor * translucent.alpha + opaqueColor * opaque.alpha * (1.0 - translucent.alpha);
    color = vec4(alpha > 0.0 ? premultiplied / alpha : vec3(0.0), alpha);
}

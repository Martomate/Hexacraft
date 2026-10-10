#define PI 3.141592653589793
#define y60 0.866025403784439

in vec2 fragPosition;

out vec4 color;

uniform mat4 invProjMatr;
uniform mat4 invViewMatr;
uniform vec3 sun;
uniform int totalSize;

uniform vec3 waterFogColor;
uniform vec3 waterAbsorption;
uniform float waterScattering;
uniform float waterDepthDarkening;
uniform float waterSurfaceAboveEye; // height of the water surface relative to the eye (in CylCoords)
uniform bool waterEffectsEnabled; // false when the eye is not at the camera position (e.g. in free fly mode)
uniform float waterFogStrength; // 1 when the eye is under water (or right above the surface), otherwise 0

uniform bool hasOcean;
uniform float seaLevelAboveEye; // height of the sea level relative to the eye (in CylCoords)
uniform vec4 waterSurfaceColor; // the average color and alpha of the texture of the water surface

// The sky is infinitely far away, but a finite distance avoids overflow and is enough to hide everything
#define SKY_DISTANCE 1000.0

// Returns how much of a ray from the eye in the given direction lies below the water surface (before reaching the sky)
float underwaterDistance(vec3 ray) {
    float surface = waterSurfaceAboveEye;
    if (surface <= 0.0) {
        // Above water: the sky is only visible where there is no terrain (i.e. beyond the render distance), and there
        // is no way of knowing if there is any water there. The world also curves away, so rays slightly below the
        // horizon would not reach the water anyway.
        return 0.0;
    }

    // Under water: the ray is under water until it reaches the surface
    return ray.y > 0.0 ? min(surface / ray.y, SKY_DISTANCE) : SKY_DISTANCE;
}

// If there is an ocean, the sky below the horizon is where the ocean would be if the world was drawn further away.
// This finds where the ray hits the sea level (if it does) and returns the normal there, or a zero vector if it misses.
// The world is curved: the sea level is a cylinder with radius R * exp(h / R) around (0, -R, 0), with x as its axis.
vec3 seaLevelNormalHitBy(vec3 ray) {
    float radius = y60 / (2.0 * PI / float(totalSize));
    float a = seaLevelAboveEye;
    if (!hasOcean || a >= 0.0 || ray.y >= 0.0) return vec3(0.0);

    // Solve |t * ray.yz - (-R, 0)|^2 = (R * exp(a / R))^2 for the smallest t, i.e. A t^2 + B t + C = 0 where
    // C = R^2 - R^2 * exp(2a / R) is computed without subtracting large numbers
    float x = 2.0 * a / radius;
    float expm1 = abs(x) < 1e-3 ? x * (1.0 + x * (0.5 + x / 6.0)) : exp(x) - 1.0;
    float A = dot(ray.yz, ray.yz);
    float B = 2.0 * radius * ray.y;
    float C = -radius * radius * expm1;
    float disc = B * B - 4.0 * A * C;
    if (disc < 0.0) return vec3(0.0); // the ray passes above the sea, since the world curves away

    float t = (-B - sqrt(disc)) / (2.0 * A);
    vec2 hit = t * ray.yz + vec2(radius, 0.0);
    return vec3(0.0, normalize(hit));
}

void main() {
    vec4 coordsBeforeMatr = vec4(fragPosition.x, fragPosition.y, -1, 1);
    vec4 coordsAfterProj = invProjMatr * coordsBeforeMatr;
    vec3 ray = normalize((invViewMatr * vec4(coordsAfterProj.xy, -1, 0)).xyz);
    vec3 sunDir = normalize(sun);
    float sunBrightness = min(exp((dot(ray, sunDir)-1)*400), 1) * 0.5;
    float sunGlow      = exp((dot(ray, sunDir)-1)*4) * 0.4;
    vec3 up = vec3(0, 1, 0);
    float rayUp = dot(ray, up);
    float gradientFalloff = 0.5;
    if (rayUp < 0) gradientFalloff = 2.0;
    vec3 col = sunBrightness * vec3(0.8, 0.65, 0.8) + sunGlow * vec3(0.8, 0.65, 0.8) + vec3(0.4, 0.7, 0.5) * (1 - abs(rayUp) * gradientFalloff) + vec3(0.0, 0.0, 0.7);

    vec3 seaNormal = seaLevelNormalHitBy(ray);
    bool eyeUnderWater = waterFogStrength > 0.0 && waterSurfaceAboveEye > 0.0;
    if (waterEffectsEnabled && !eyeUnderWater && seaNormal != vec3(0.0)) {
        // Look like the water surface (as drawn by the block shader and the world combiner) on top of deep water
        float cosTheta = abs(dot(seaNormal, ray));
        float fresnel = pow(1.0 - cosTheta, 5.0);
        float alpha = mix(waterSurfaceColor.a, 1.0, fresnel);
        float visibility = max(dot(seaNormal, sunDir), 0) * 0.2 + 0.8;
        col = mix(waterFogColor, waterSurfaceColor.rgb * visibility, alpha);
    }

    if (waterFogStrength > 0.0) {
        // Same as in the world combiner: absorption (red first) and then light scattered by the water itself,
        // which is darker the deeper the eye is
        float dist = underwaterDistance(ray) * waterFogStrength;
        float eyeDepth = max(waterSurfaceAboveEye * waterFogStrength, 0.0);
        vec3 fogColor = waterFogColor * exp(-(waterAbsorption + waterDepthDarkening) * eyeDepth);
        vec3 transmittance = exp(-waterAbsorption * dist);
        float scattered = 1.0 - exp(-waterScattering * dist);
        col = mix(col * transmittance, fogColor, scattered);
    }

    color = vec4(col, 1);
}
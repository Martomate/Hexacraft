in vec2 fragPosition;

out vec4 color;

uniform mat4 invProjMatr;
uniform mat4 invViewMatr;
uniform vec3 sun;

uniform vec3 waterFogColor;
uniform vec3 waterAbsorption;
uniform float waterSurfaceAboveEye; // height of the water surface relative to the eye (in CylCoords)
uniform float waterFogStrength; // 0 when there is no water around the eye, 1 when the eye is under water

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

    if (waterFogStrength > 0.0) {
        vec3 transmittance = exp(-waterAbsorption * underwaterDistance(ray) * waterFogStrength);
        col = col * transmittance + waterFogColor * (1.0 - transmittance);
    }

    color = vec4(col, 1);
}
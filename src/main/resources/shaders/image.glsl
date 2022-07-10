#pragma shader vert

in vec2 position;

out vec2 textureCoords;

uniform mat4 transformationMatrix;
uniform vec2 windowSize;
uniform vec2 imageSize;

float aspectRatio = windowSize.x / windowSize.y;
float aspectSize = imageSize.x / imageSize.y;

void main() {
	gl_Position = transformationMatrix * vec4(position, 0.0, 1.0);
	gl_Position.xy *= vec2(1 / aspectRatio, 1);
	textureCoords = vec2(position.x, 1 - position.y);
}

#pragma shader frag

in vec2 textureCoords;

out vec4 color;

uniform sampler2D textureIn;
uniform float alpha = 1;

void main() {
    vec4 texCol = texture(textureIn, textureCoords);
	color = vec4(texCol.rgb, texCol.a * alpha);
}
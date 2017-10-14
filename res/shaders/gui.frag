
in vec2 textureCoords;

out vec4 color;

uniform sampler2D textureIn;

void main() {
	color = vec4(texture(textureIn, textureCoords).rgb, 0.5);
}
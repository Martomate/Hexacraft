
in vec2 textureCoords;

out vec4 color;

uniform sampler2D textureIn;
uniform float alpha = 1;

void main() {
    vec4 texCol = texture(textureIn, textureCoords);
	color = vec4(texCol.rgb, texCol.a * alpha);
}
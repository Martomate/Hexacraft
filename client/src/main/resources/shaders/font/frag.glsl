in vec2 pass_textureCoords;

out vec4 out_color;

uniform vec3 color;
uniform sampler2D font;

void main(void){
    out_color = vec4(color, texture(font, pass_textureCoords).a);
}
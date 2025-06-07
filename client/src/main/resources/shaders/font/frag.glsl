in vec2 pass_textureCoords;

out vec4 out_color;

uniform vec4 color;
uniform sampler2D font;

void main(void){
    out_color = vec4(color.rgb, texture(font, pass_textureCoords).a * color.a);
}
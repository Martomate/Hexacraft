#pragma shader vert
in vec2 position;
in vec2 textureCoords;

out vec2 pass_textureCoords;

uniform vec2 translation;
uniform float windowAspectRatio;

/*
const float rotation = 3.141592 * 0;
const mat2 rotMat = mat2(cos(rotation), sin(rotation), -sin(rotation), cos(rotation));
*/
void main(void){
    gl_Position = vec4(/*rotMat * */(position + vec2(translation.x, translation.y)) * vec2(1 / windowAspectRatio, 1), 0, 1);
    pass_textureCoords = textureCoords;
}

#pragma shader frag
in vec2 pass_textureCoords;

out vec4 out_color;

uniform vec3 color;
uniform sampler2D font;

void main(void){
    out_color = vec4(color, texture(font, pass_textureCoords).a);
}
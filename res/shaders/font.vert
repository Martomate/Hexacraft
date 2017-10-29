in vec2 position;
in vec2 textureCoords;

out vec2 pass_textureCoords;

uniform vec2 translation;

const float rotation = 3.141592 * 0;
const float aspectRatio = 16.0 / 9.0;
const mat2 rotMat = mat2(cos(rotation), sin(rotation), -sin(rotation), cos(rotation));

void main(void){
    gl_Position = vec4(/*rotMat * */(position + vec2(translation.x * 2, (translation.y - 1) * 2))/* * vec2(1, aspectRatio)*/, 0, 1);
    pass_textureCoords = textureCoords;
}
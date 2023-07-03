package com.martomate.hexacraft.font.mesh

import com.martomate.hexacraft.infra.gpu.OpenGL
import java.net.URL

/** Represents a font. It holds the font's texture atlas as well as having the ability to create the
  * quad vertices for any text using this font.
  *
  * @author
  *   Karl
  */
class FontType(val textureAtlas: OpenGL.TextureId, val loader: TextMeshCreator) {

  /** Takes in an unloaded text and calculate all of the vertices for the quads on which this text
    * will be rendered. The vertex positions and texture coords and calculated based on the
    * information from the font file.
    *
    * @param text
    *   the unloaded text.
    * @return
    *   Information about the vertices of all the quads.
    */
  def loadText(text: GUIText): TextMeshData = loader.createTextMesh(text)
}

object FontType {

  /** Creates a new font and loads up the data about each character from the font file.
    *
    * @param textureAtlas
    *   the ID of the font atlas texture.
    * @param fontFile
    *   the font file containing information about each character in the texture atlas.
    */
  def fromUrl(textureAtlas: OpenGL.TextureId, fontFile: URL): FontType =
    new FontType(textureAtlas, new TextMeshCreator(fontFile))
}

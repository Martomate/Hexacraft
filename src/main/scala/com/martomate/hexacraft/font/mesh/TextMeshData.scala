package com.martomate.hexacraft.font.mesh

/** Stores the vertex data for all the quads on which a text will be rendered.
  *
  * @author
  *   Karl
  */
class TextMeshData(val vertexPositions: Seq[Float], val textureCoords: Seq[Float]) {
  def getVertexCount: Int = vertexPositions.length / 2
}

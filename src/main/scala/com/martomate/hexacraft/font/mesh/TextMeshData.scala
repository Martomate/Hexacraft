package com.martomate.hexacraft.font.mesh

/** Stores the vertex data for all the quads on which a text will be rendered.
  *
  * @author
  *   Karl
  */
class TextMeshData(val vertexPositions: Array[Float], val textureCoords: Array[Float]) {
  def getVertexCount: Int = vertexPositions.length / 2
}

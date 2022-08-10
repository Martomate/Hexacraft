package com.martomate.hexacraft.font.mesh

/**
 * Stores the vertex data for all the quads on which a text will be rendered.
 *
 * @author Karl
 *
 */
class TextMeshData (var vertexPositions: Array[Float], var textureCoords: Array[Float]) {
  def getVertexPositions: Array[Float] = vertexPositions

  def getTextureCoords: Array[Float] = textureCoords

  def getVertexCount: Int = vertexPositions.length / 2
}
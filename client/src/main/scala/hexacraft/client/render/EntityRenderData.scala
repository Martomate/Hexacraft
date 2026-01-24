package hexacraft.client.render

import hexacraft.shaders.EntityShader
import hexacraft.util.InlinedIterable
import hexacraft.world.{BlocksInWorld, ChunkCache, CylinderSize}
import hexacraft.world.coord.{CoordUtils, CylCoords}
import hexacraft.world.entity.{Entity, EntityPart}

import org.joml.{Matrix4f, Vector4f}

import scala.collection.mutable

object EntityRenderData {
  def fromEntities(
      entities: Iterable[Entity],
      world: BlocksInWorld
  )(using CylinderSize): IndexedSeq[EntityRenderData] = {
    val chunkCache = new ChunkCache(world)

    val tr = new Matrix4f

    val pieces = mutable.ArrayBuffer.empty[EntityRenderData]

    for ent <- InlinedIterable(entities) if ent.model.isDefined do {
      val baseT = ent.transform.transform
      val model = ent.model.get

      for part <- InlinedIterable(model.parts) do {
        baseT.mul(part.transform, tr)

        val coords4 = tr.transform(new Vector4f(0, 0.5f, 0, 1))
        val blockCoords = CylCoords(coords4.x, coords4.y, coords4.z).toBlockCoords
        val coords = CoordUtils.getEnclosingBlock(blockCoords)._1
        val cCoords = coords.getChunkRelWorld

        val partChunk = chunkCache.getChunk(cCoords)

        val brightness: Float =
          if partChunk != null then {
            partChunk.getBrightness(coords.getBlockRelChunk)
          } else {
            0
          }

        pieces += EntityRenderData(new Matrix4f(tr), part, brightness)
      }
    }

    pieces.toIndexedSeq
  }
}

class EntityRenderData(tr: Matrix4f, part: EntityPart, brightness: Float) {
  def getInstanceData(side: Int): EntityShader.InstanceData = {
    EntityShader.InstanceData(
      modelMatrix = new Matrix4f(tr),
      texOffset = part.textureOffset(side),
      texSize = part.textureSize(side),
      blockTex = part.texture(side),
      brightness
    )
  }
}

package hexacraft.client.render

import hexacraft.client.entity.{EntitySkin, ModelComponent}
import hexacraft.shaders.EntityShader
import hexacraft.util.{InlinedIterable, Loop}
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

    for {
      ent <- InlinedIterable(entities)
      modelComponent <- ent.accessComponent { case c: ModelComponent => c }
    } do {
      val baseT = ent.transform.transform
      val parts = modelComponent.model.parts
      val prismTransforms = modelComponent.pose.prismTransforms

      Loop.rangeUntil(0, parts.size) { idx =>
        val part = parts(idx)
        if part.isVisible then {
          baseT.mul(prismTransforms(idx), tr)

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

          pieces += EntityRenderData(new Matrix4f(tr), part, modelComponent.skin, brightness)
        }
      }
    }

    pieces.toIndexedSeq
  }
}

class EntityRenderData(tr: Matrix4f, part: EntityPart, skin: EntitySkin, brightness: Float) {
  def getInstanceData(side: Int): EntityShader.InstanceData = {
    EntityShader.InstanceData(
      modelMatrix = new Matrix4f(tr),
      texOffset = skin.textureOffset(part, side),
      texSize = skin.textureSize(part, side),
      blockTex = skin.texture(side),
      brightness
    )
  }
}

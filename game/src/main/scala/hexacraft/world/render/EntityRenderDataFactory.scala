package hexacraft.world.render

import hexacraft.shaders.EntityShader
import hexacraft.world.{BlocksInWorld, ChunkCache, CylinderSize}
import hexacraft.world.coord.{CoordUtils, CylCoords}
import hexacraft.world.entity.{Entity, EntityModel}

import org.joml.{Matrix4f, Vector4f}

object EntityRenderDataFactory {
  def getEntityRenderData(entities: Iterable[Entity], side: Int, world: BlocksInWorld)(using
      CylinderSize
  ): Iterable[(EntityModel, Seq[EntityShader.InstanceData])] = {
    val chunkCache = new ChunkCache(world)

    val tr = new Matrix4f

    for ent <- entities if ent.model.isDefined yield {
      val baseT = ent.transform.transform
      val model = ent.model.get

      val parts = for part <- model.parts yield {
        baseT.mul(part.transform, tr)

        val coords4 = tr.transform(new Vector4f(0, 0.5f, 0, 1))
        val blockCoords = CylCoords(coords4.x, coords4.y, coords4.z).toBlockCoords
        val coords = CoordUtils.getEnclosingBlock(blockCoords)._1
        val cCoords = coords.getChunkRelWorld

        val partChunk = chunkCache.getChunk(cCoords)

        val brightness: Float =
          if partChunk != null then {
            partChunk.lighting.getBrightness(coords.getBlockRelChunk)
          } else {
            0
          }

        EntityShader.InstanceData(
          modelMatrix = new Matrix4f(tr),
          texOffset = part.textureOffset(side),
          texSize = part.textureSize(side),
          blockTex = part.texture(side),
          brightness
        )
      }

      (model, parts)
    }
  }
}

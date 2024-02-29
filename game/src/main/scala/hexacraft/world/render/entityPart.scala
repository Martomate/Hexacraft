package hexacraft.world.render

import hexacraft.world.{BlocksInWorld, ChunkCache, CylinderSize}
import hexacraft.world.coord.{CoordUtils, CylCoords}
import hexacraft.world.entity.{Entity, EntityModel}

import org.joml.{Matrix4f, Vector4f}

import java.nio.ByteBuffer

case class EntityDataForShader(model: EntityModel, parts: Seq[EntityPartDataForShader])

case class EntityPartDataForShader(
    modelMatrix: Matrix4f,
    texOffset: (Int, Int),
    texSize: (Int, Int),
    blockTex: Int,
    brightness: Float
) {
  def fill(buf: ByteBuffer): Unit = {
    modelMatrix.get(buf)
    buf.position(buf.position() + 16 * 4)
    buf.putInt(texOffset._1)
    buf.putInt(texOffset._2)
    buf.putInt(texSize._1)
    buf.putInt(texSize._2)
    buf.putInt(blockTex)
    buf.putFloat(brightness)
  }
}

object EntityRenderDataFactory {
  def getEntityRenderData(entities: Iterable[Entity], side: Int, world: BlocksInWorld)(using
      CylinderSize
  ): Iterable[EntityDataForShader] = {
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

        EntityPartDataForShader(
          modelMatrix = new Matrix4f(tr),
          texOffset = part.textureOffset(side),
          texSize = part.textureSize(side),
          blockTex = part.texture(side),
          brightness
        )
      }

      EntityDataForShader(model, parts)
    }
  }
}

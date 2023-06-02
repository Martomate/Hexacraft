package com.martomate.hexacraft.world.render

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.{BlocksInWorld, ChunkCache}
import com.martomate.hexacraft.world.chunk.EntitiesInChunk
import com.martomate.hexacraft.world.coord.CoordUtils
import com.martomate.hexacraft.world.coord.fp.CylCoords

import org.joml.{Matrix4f, Vector4f}

object EntityRenderDataFactory {
  def getEntityRenderData(entities: EntitiesInChunk, side: Int, world: BlocksInWorld)(using
      CylinderSize
  ): Iterable[EntityDataForShader] =
    val chunkCache = new ChunkCache(world)

    val tr = new Matrix4f

    for ent <- entities.allEntities yield
      val baseT = ent.transform
      val model = ent.model

      val parts = for part <- model.parts yield
        baseT.mul(part.transform, tr)

        val coords4 = tr.transform(new Vector4f(0, 0.5f, 0, 1))
        val blockCoords = CylCoords(coords4.x, coords4.y, coords4.z).toBlockCoords
        val coords = CoordUtils.getEnclosingBlock(blockCoords)._1
        val cCoords = coords.getChunkRelWorld

        val partChunk = chunkCache.getChunk(cCoords)

        val brightness: Float =
          if partChunk != null
          then partChunk.lighting.getBrightness(coords.getBlockRelChunk)
          else 0

        EntityPartDataForShader(
          modelMatrix = new Matrix4f(tr),
          texOffset = part.textureOffset(side),
          texSize = part.textureSize(side),
          blockTex = part.texture(side),
          brightness
        )

      EntityDataForShader(model, parts)

}

package com.martomate.hexacraft.world.temp

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.chunk.ChunkEventListener
import com.martomate.hexacraft.world.gen.WorldGenerator
import com.martomate.hexacraft.world.settings.WorldSettingsProvider

trait IWorld extends ChunkEventListener with BlockSetAndGet with BlocksInWorld {
  def size: CylinderSize
  def worldSettings: WorldSettingsProvider
  def worldGenerator: WorldGenerator
  def renderDistance: Double

  def getHeight(x: Int, z: Int): Int

  private[temp] def chunkAddedOrRemovedListeners: Iterable[ChunkAddedOrRemovedListener]
  def addChunkAddedOrRemovedListener(listener: ChunkAddedOrRemovedListener): Unit
}

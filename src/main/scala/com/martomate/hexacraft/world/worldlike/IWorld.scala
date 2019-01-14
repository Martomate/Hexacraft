package com.martomate.hexacraft.world.worldlike

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.setget.BlockSetAndGet
import com.martomate.hexacraft.world.chunk.ChunkAddedOrRemovedListener
import com.martomate.hexacraft.world.collision.CollisionDetector
import com.martomate.hexacraft.world.column.ChunkColumnListener
import com.martomate.hexacraft.world.gen.WorldGenerator
import com.martomate.hexacraft.world.settings.WorldSettingsProvider

trait IWorld extends BlockSetAndGet with BlocksInWorld with ChunkColumnListener {
  val size: CylinderSize
  def worldSettings: WorldSettingsProvider
  def worldGenerator: WorldGenerator
  def renderDistance: Double

  def collisionDetector: CollisionDetector

  def getHeight(x: Int, z: Int): Int

  def addChunkAddedOrRemovedListener(listener: ChunkAddedOrRemovedListener): Unit
  def removeChunkAddedOrRemovedListener(listener: ChunkAddedOrRemovedListener): Unit
}

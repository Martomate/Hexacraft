package com.martomate.hexacraft.world.entity.ai

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.BlocksInWorld
import com.martomate.hexacraft.world.block.Block
import com.martomate.hexacraft.world.coord.CoordUtils
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.coord.integer.BlockRelWorld

import org.joml.Vector3d

class SimpleAIInput(using CylinderSize):
  def blockInFront(world: BlocksInWorld, position: CylCoords, rotation: Vector3d, dist: Double): Block =
    world.getBlock(blockInFrontCoords(position, rotation, dist)).blockType

  private def blockInFrontCoords(position: CylCoords, rotation: Vector3d, dist: Double): BlockRelWorld =
    val coords = position.offset(dist * math.cos(rotation.y), 0, dist * -math.sin(rotation.y))
    CoordUtils.getEnclosingBlock(coords.toBlockCoords)._1

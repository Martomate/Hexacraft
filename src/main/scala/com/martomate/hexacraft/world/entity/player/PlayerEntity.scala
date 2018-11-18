package com.martomate.hexacraft.world.entity.player

import com.martomate.hexacraft.world.block.{Block, HexBox}
import com.martomate.hexacraft.world.coord.CoordUtils
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.coord.integer.BlockRelWorld
import com.martomate.hexacraft.world.entity.EntityModel
import com.martomate.hexacraft.world.entity.base.BasicEntity
import com.martomate.hexacraft.world.entity.player.ai.{PlayerAI, SimplePlayerAI}
import com.martomate.hexacraft.world.worldlike.IWorld

class PlayerEntity(_initPos: CylCoords, _model: EntityModel, _world: IWorld) extends BasicEntity(_initPos, _model, _world) {
  override val boundingBox: HexBox = new HexBox(0.2f, 0, 1.75f)

  private val ai: PlayerAI = new SimplePlayerAI(this)

  override def tick(): Unit = {
    ai.tick()
    velocity.add(ai.acceleration())

    velocity.x *= 0.9
    velocity.z *= 0.9

    super.tick()
  }

  def coordsAtOffset(dx: Double, dy: Double, dz: Double): BlockRelWorld = CoordUtils.toBlockCoords(CylCoords(position.x + dx, position.y + dy, position.z + dz)(position.cylSize).toBlockCoords)._1

  def blockInFront(dist: Double): Block = _world.getBlock(coordsAtOffset(dist * math.cos(rotation.y), 0, dist * -math.sin(rotation.y))).blockType
}

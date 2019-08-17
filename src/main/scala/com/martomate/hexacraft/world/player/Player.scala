package com.martomate.hexacraft.world.player

import com.flowpowered.nbt.{ByteTag, CompoundTag, ShortTag}
import com.martomate.hexacraft.util.NBTUtil
import com.martomate.hexacraft.world.block.{Block, HexBox}
import com.martomate.hexacraft.world.coord.fp.BlockCoords
import com.martomate.hexacraft.world.worldlike.IWorld
import org.joml.Vector3d

class Player(val world: IWorld) {
  import world.size.impl

  val bounds = new HexBox(0.2f, -1.65f, 0.1f)
  val velocity = new Vector3d
  val position = new Vector3d
  val rotation = new Vector3d
  var flying = false
  var selectedItemSlot: Int = 0

  val inventory = new Inventory

  def blockInHand: Block = inventory(selectedItemSlot)// TODO: temporary, make inventory system

  initPosition()

  private def initPosition(): Unit = {
    val startX = (math.random * 100 - 50).toInt
    val startZ = (math.random * 100 - 50).toInt
    val startCoords = BlockCoords(startX, world.getHeight(startX, startZ), startZ).toCylCoords
    position.set(startCoords.x, startCoords.y - bounds.bottom + 2, startCoords.z)
  }
  
  def toNBT: CompoundTag = {
    NBTUtil.makeCompoundTag("player", Seq(
      NBTUtil.makeVectorTag("position", position),
      NBTUtil.makeVectorTag("rotation", rotation),
      NBTUtil.makeVectorTag("velocity", velocity),
      new ByteTag("flying", flying),
      new ShortTag("selectedItemSlot", selectedItemSlot.toShort)
    ))
  }
  
  def fromNBT(tag: CompoundTag): Unit = {
    if (tag != null) {
      NBTUtil.getCompoundTag(tag, "position").foreach(p => NBTUtil.setVector(p, position))
      NBTUtil.getCompoundTag(tag, "rotation").foreach(p => NBTUtil.setVector(p, rotation))
      NBTUtil.getCompoundTag(tag, "velocity").foreach(p => NBTUtil.setVector(p, velocity))

      flying = NBTUtil.getByte(tag, "flying", 0) != 0
      selectedItemSlot = NBTUtil.getShort(tag, "selectedItemSlot", 0)
    }
  }
}

package com.martomate.hexacraft.world.player

import com.flowpowered.nbt.{ByteTag, CompoundTag, ShortTag}
import com.martomate.hexacraft.nbt.{Nbt, NBTUtil}
import com.martomate.hexacraft.world.block.{Block, Blocks, HexBox}
import com.martomate.hexacraft.world.coord.fp.CylCoords
import org.joml.Vector3d

class Player(val inventory: Inventory) {
  val bounds = new HexBox(0.2f, -1.65f, 0.1f)
  val velocity = new Vector3d
  val position = new Vector3d
  val rotation = new Vector3d
  var flying = false
  var selectedItemSlot: Int = 0

  def blockInHand: Block = inventory(selectedItemSlot) // TODO: temporary, make inventory system

  def toNBT: CompoundTag = {
    NBTUtil.makeCompoundTag(
      "player",
      Seq(
        NBTUtil.makeVectorTag("position", position),
        NBTUtil.makeVectorTag("rotation", rotation),
        NBTUtil.makeVectorTag("velocity", velocity),
        new ByteTag("flying", flying),
        new ShortTag("selectedItemSlot", selectedItemSlot.toShort),
        NBTUtil.makeCompoundTag("inventory", inventory.toNBT)
      )
    )
  }
}

object Player {
  def atStartPos(initialFootCoords: CylCoords)(using Blocks): Player = {
    val player = new Player(Inventory.default)
    player.position.set(
      initialFootCoords.x,
      initialFootCoords.y - player.bounds.bottom,
      initialFootCoords.z
    )
    player
  }

  def fromNBT(tag2: CompoundTag)(using Blocks): Player = {
    val tag = Nbt.from(tag2)
    val inventory =
      tag.getCompoundTag("inventory") match
        case Some(tag) => Inventory.fromNBT(tag)
        case None      => Inventory.default

    val player = new Player(inventory)

    tag.getCompoundTag("position").foreach(p => p.setVector(player.position))
    tag.getCompoundTag("rotation").foreach(p => p.setVector(player.rotation))
    tag.getCompoundTag("velocity").foreach(p => p.setVector(player.velocity))

    player.flying = tag.getByte("flying", 0) != 0
    player.selectedItemSlot = tag.getShort("selectedItemSlot", 0)

    player
  }
}

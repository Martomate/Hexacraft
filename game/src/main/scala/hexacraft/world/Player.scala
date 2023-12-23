package hexacraft.world

import hexacraft.world.block.Block
import hexacraft.world.coord.CylCoords

import com.martomate.nbt.Nbt
import org.joml.Vector3d

class Player(var inventory: Inventory) {
  val bounds = new HexBox(0.2f, -1.65f, 0.1f)
  val velocity = new Vector3d
  val position = new Vector3d
  val rotation = new Vector3d
  var flying = false
  var selectedItemSlot: Int = 0

  def blockInHand: Block = inventory(selectedItemSlot) // TODO: temporary, make inventory system

  def toNBT: Nbt.MapTag = {
    Nbt
      .makeMap(
        "position" -> Nbt.makeVectorTag(position),
        "rotation" -> Nbt.makeVectorTag(rotation),
        "velocity" -> Nbt.makeVectorTag(velocity),
        "flying" -> Nbt.ByteTag(flying),
        "selectedItemSlot" -> Nbt.ShortTag(selectedItemSlot.toShort),
        "inventory" -> inventory.toNBT
      )
  }
}

object Player {
  def atStartPos(initialFootCoords: CylCoords): Player = {
    val player = new Player(Inventory.default)
    player.position.set(
      initialFootCoords.x,
      initialFootCoords.y - player.bounds.bottom,
      initialFootCoords.z
    )
    player
  }

  def fromNBT(tag: Nbt.MapTag): Player = {
    val inventory =
      tag.getMap("inventory") match
        case Some(tag) => Inventory.fromNBT(tag)
        case None      => Inventory.default

    val player = new Player(inventory)

    tag.getMap("position").foreach(p => p.setVector(player.position))
    tag.getMap("rotation").foreach(p => p.setVector(player.rotation))
    tag.getMap("velocity").foreach(p => p.setVector(player.velocity))

    player.flying = tag.getByte("flying", 0) != 0
    player.selectedItemSlot = tag.getShort("selectedItemSlot", 0)

    player
  }
}

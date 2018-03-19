package hexacraft.world

import com.flowpowered.nbt.{ByteTag, CompoundTag, DoubleTag, ShortTag}
import hexacraft.HexBox
import hexacraft.util.NBTUtil
import hexacraft.world.coord.BlockCoords
import hexacraft.world.storage.World
import org.joml.Vector3d

class Player(val world: World) {
  val bounds = new HexBox(0.2f, -1.65f, 0.1f)
  val velocity = new Vector3d
  val position = new Vector3d
  val rotation = new Vector3d
  var flying = false
  var selectedItemSlot: Int = 0

  val inventory = new Inventory

  def blockInHand = inventory(selectedItemSlot)// TODO: temporary, make inventory system
  
  {
    val startX = (math.random * 100 - 50).toInt
    val startZ = (math.random * 100 - 50).toInt
    val startCoords = BlockCoords(startX, world.getHeight(startX, startZ), startZ, world.size).toCylCoords
    position.set(startCoords.x, startCoords.y - bounds.bottom + 2, startCoords.z)
  }
  
  def toNBT: CompoundTag = {
    def makeVectorTag(name: String, vector: Vector3d): CompoundTag = NBTUtil.makeCompoundTag(name, Seq(
        new DoubleTag("x", vector.x),
        new DoubleTag("y", vector.y),
        new DoubleTag("z", vector.z)
    ))
    
    NBTUtil.makeCompoundTag("player", Seq(
        makeVectorTag("position", position),
        makeVectorTag("rotation", rotation),
        makeVectorTag("velocity", velocity),
        new ByteTag("flying", flying),
        new ShortTag("selectedItemSlot", selectedItemSlot.toShort)
    ))
  }
  
  def fromNBT(tag: CompoundTag): Unit = {
    def setVector(tag: CompoundTag, vector: Vector3d) = {
      val x = NBTUtil.getDouble(tag, "x", vector.x)
      val y = NBTUtil.getDouble(tag, "y", vector.y)
      val z = NBTUtil.getDouble(tag, "z", vector.z)
      vector.set(x, y, z)
    }
    
    if (tag != null) {
      NBTUtil.getCompoundTag(tag, "position").foreach(p => setVector(p, position))
      NBTUtil.getCompoundTag(tag, "rotation").foreach(p => setVector(p, rotation))
      NBTUtil.getCompoundTag(tag, "velocity").foreach(p => setVector(p, velocity))
      tag.getValue.get("flying") match {
        case t: ByteTag => flying = t.getValue.byteValue() != 0
        case _ =>
      }
      tag.getValue.get("selectedItemSlot") match {
        case s: ShortTag => selectedItemSlot = s.getValue.shortValue()
        case _ =>
      }
    }
  }
}

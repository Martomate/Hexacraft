package hexacraft.world

import hexacraft.nbt.Nbt
import hexacraft.world.block.Block

case class Inventory(slots: Map[Int, Block]) {
  def apply(idx: Int): Block = slots.getOrElse(idx, Block.Air)

  def firstEmptySlot: Option[Int] = (0 until 4 * 9).find(i => this(i) == Block.Air)

  def updated(idx: Int, block: Block): Inventory = Inventory(slots.updated(idx, block))

  def toNBT: Nbt.MapTag = {
    Nbt.makeMap("slots" -> Nbt.ListTag(slotsToNbt))
  }

  private def slotsToNbt: Seq[Nbt.MapTag] = {
    for (idx, block) <- slots.toSeq if block != Block.Air yield {
      val idxTag = Nbt.ByteTag(idx.toByte)
      val blockIdTag = Nbt.ByteTag(block.id)

      Nbt.makeMap("slot" -> idxTag, "id" -> blockIdTag)
    }
  }
}

object Inventory {
  def default: Inventory = {
    val slots = Map(
      0 -> Block.Dirt,
      1 -> Block.Grass,
      2 -> Block.Sand,
      3 -> Block.Stone,
      4 -> Block.Water,
      5 -> Block.OakLog,
      6 -> Block.OakLeaves,
      7 -> Block.Planks,
      8 -> Block.BirchLog,
      9 -> Block.BirchLeaves,
      10 -> Block.Tnt
    )
    Inventory(slots)
  }

  def fromNBT(nbt: Nbt.MapTag): Inventory = {
    nbt.getList("slots") match {
      case Some(slotTags) =>
        val slots = slotTags.flatMap {
          case s: Nbt.MapTag =>
            val idx = s.getByte("slot", -1)
            val id = s.getByte("id", -1)

            if idx != -1 && id != -1 then {
              Some(idx.toInt -> Block.byId(id))
            } else {
              None
            }
          case _ => None
        }
        Inventory(Map.from(slots))
      case None =>
        Inventory(Map.empty)
    }
  }
}

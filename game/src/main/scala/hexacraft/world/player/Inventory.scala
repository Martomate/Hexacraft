package hexacraft.world.player

import hexacraft.world.block.Block

import com.martomate.nbt.Nbt

case class Inventory(slots: Map[Int, Block]) {
  def apply(idx: Int): Block = slots.getOrElse(idx, Block.Air)

  def firstEmptySlot: Option[Int] = (0 until 4 * 9).find(i => this(i) == Block.Air)

  def updated(idx: Int, block: Block): Inventory = Inventory(slots.updated(idx, block))

  def toNBT: Nbt.MapTag =
    Nbt.makeMap(
      "slots" ->
        Nbt
          .ListTag(
            for
              (idx, block) <- slots.toSeq
              if block != Block.Air
            yield Nbt.makeMap(
              "slot" -> Nbt.ByteTag(idx.toByte),
              "id" -> Nbt.ByteTag(block.id)
            )
          )
    )
}

object Inventory {
  def default: Inventory =
    Inventory(
      Map(
        0 -> Block.Dirt,
        1 -> Block.Grass,
        2 -> Block.Sand,
        3 -> Block.Stone,
        4 -> Block.Water,
        5 -> Block.Log,
        6 -> Block.Leaves,
        7 -> Block.Planks,
        8 -> Block.BirchLog,
        9 -> Block.BirchLeaves,
        10 -> Block.Tnt
      )
    )

  def fromNBT(nbt: Nbt.MapTag): Inventory =
    nbt.getList("slots") match
      case Some(slotTags) =>
        val slots = slotTags.flatMap {
          case s: Nbt.MapTag =>
            val idx = s.getByte("slot", -1)
            val id = s.getByte("id", -1)

            if idx != -1 && id != -1
            then Some(idx.toInt -> Block.byId(id))
            else None
          case _ => None
        }
        Inventory(Map.from(slots))
      case None => Inventory(Map.empty)
}

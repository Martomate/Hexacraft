package hexacraft.world.player

import hexacraft.nbt.Nbt
import hexacraft.util.{EventDispatcher, RevokeTrackerFn, Tracker}
import hexacraft.world.block.Block

class Inventory(init_slots: Map[Int, Block]) {
  private val dispatcher = new EventDispatcher[Unit]
  def trackChanges(tracker: Tracker[Unit]): RevokeTrackerFn = dispatcher.track(tracker)

  private val slots: Array[Block] = Array.fill(4 * 9)(Block.Air)
  for ((idx, block) <- init_slots) slots(idx) = block

  def apply(idx: Int): Block = slots(idx)

  def firstEmptySlot: Option[Int] = (0 until 4 * 9).find(i => this(i) == Block.Air)

  def update(idx: Int, block: Block): Unit =
    slots(idx) = block
    dispatcher.notify(())

  def toNBT: Nbt.MapTag =
    Nbt.makeMap(
      "slots" ->
        Nbt
          .ListTag(
            slots.toSeq.zipWithIndex
              .filter((block, _) => block.id != Block.Air.id)
              .map((block, idx) => Nbt.makeMap("slot" -> Nbt.ByteTag(idx.toByte), "id" -> Nbt.ByteTag(block.id)))
          )
    )

  override def equals(obj: Any): Boolean =
    obj match
      case inv: Inventory =>
        inv.slots.sameElements(this.slots)
      case _ => false
}

object Inventory {
  def default: Inventory =
    new Inventory(
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
        new Inventory(Map.from(slots))
      case None => new Inventory(Map.empty)
}

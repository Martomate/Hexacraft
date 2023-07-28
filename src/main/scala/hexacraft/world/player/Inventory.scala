package hexacraft.world.player

import hexacraft.nbt.{Nbt, NBTUtil}
import hexacraft.util.{EventDispatcher, RevokeTrackerFn, Tracker}
import hexacraft.world.block.{Block, Blocks}

import com.flowpowered.nbt.*

import scala.collection.mutable.ArrayBuffer

class Inventory(init_slots: Map[Int, Block])(using Blocks: Blocks) {
  private val dispatcher = new EventDispatcher[Unit]
  def trackChanges(tracker: Tracker[Unit]): RevokeTrackerFn = dispatcher.track(tracker)

  private val slots: Array[Block] = Array.fill(4 * 9)(Blocks.Air)
  for ((idx, block) <- init_slots) slots(idx) = block

  def apply(idx: Int): Block = slots(idx)

  def update(idx: Int, block: Block): Unit =
    slots(idx) = block
    dispatcher.notify(())

  def toNBT: Seq[Tag[_]] =
    Seq(
      Nbt
        .ListTag(
          slots.toSeq.zipWithIndex
            .filter((block, _) => block.id != Blocks.Air.id)
            .map((block, idx) => Nbt.makeMap("slot" -> Nbt.ByteTag(idx.toByte), "id" -> Nbt.ByteTag(block.id)))
        )
        .toRaw("slots")
    )

  override def equals(obj: Any): Boolean =
    obj match
      case inv: Inventory =>
        inv.slots.sameElements(this.slots)
      case _ => false
}

object Inventory {
  def default(using Blocks: Blocks): Inventory =
    new Inventory(
      Map(
        0 -> Blocks.Dirt,
        1 -> Blocks.Grass,
        2 -> Blocks.Sand,
        3 -> Blocks.Stone,
        4 -> Blocks.Water,
        5 -> Blocks.Log,
        6 -> Blocks.Leaves,
        7 -> Blocks.Planks,
        8 -> Blocks.BirchLog,
        9 -> Blocks.BirchLeaves
      )
    )

  def fromNBT(nbt: Nbt.MapTag)(using Blocks): Inventory =
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

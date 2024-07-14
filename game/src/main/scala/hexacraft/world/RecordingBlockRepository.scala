package hexacraft.world

import hexacraft.world.block.{BlockRepository, BlockState}
import hexacraft.world.coord.BlockRelWorld

import scala.collection.mutable

class RecordingBlockRepository(world: BlockRepository) extends BlockRepository {
  private val updates = mutable.ArrayBuffer.empty[BlockRelWorld]
  def collectUpdates: Seq[BlockRelWorld] = {
    val res = updates.toSeq
    updates.clear()
    res
  }

  override def getBlock(coords: BlockRelWorld): BlockState = {
    world.getBlock(coords)
  }

  override def setBlock(coords: BlockRelWorld, block: BlockState): Unit = {
    updates += coords
    world.setBlock(coords, block)
  }

  override def removeBlock(coords: BlockRelWorld): Unit = {
    updates += coords
    world.removeBlock(coords)
  }
}

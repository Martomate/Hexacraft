package com.martomate.hexacraft.world.gen.feature

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.state.BlockState
import com.martomate.hexacraft.world.block.{Block, Blocks}
import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, Offset}
import com.martomate.hexacraft.world.gen.PlannedWorldChange

class GenTree(at: BlockRelWorld)(implicit cylSize: CylinderSize) {
  private type BlockSpec = (Offset, Block)

  private def makePillar(x: Int, y: Int, z: Int, len: Int, b: Block): Seq[BlockSpec] =
    for (yy <- y until y + len) yield Offset(x, yy, z) -> b

  private def makePlatform(x: Int, y: Int, z: Int, r: Int, b: Block): Seq[BlockSpec] = {
    for {
      dx <- -r to r
      dz <- -r to r
      if math.abs(dx + dz) <= r
    } yield Offset(x + dx, y, z + dz) -> b
  }

  private val blocks: Seq[BlockSpec] =
    makePlatform(0, 6, 0, 1, Blocks.Leaves) ++
    makePlatform(0, 7, 0, 2, Blocks.Leaves) ++
    makePlatform(0, 8, 0, 1, Blocks.Leaves) ++
    makePlatform(0, 9, 0, 0, Blocks.Leaves) ++
    makePillar(0, 0, 0, 9, Blocks.Log)

  def generate(): PlannedWorldChange = {
    val worldChange = new PlannedWorldChange
    for ((c, b) <- blocks) {
      setBlockAt(at, c, b)(worldChange)
    }
    worldChange
  }

  private def setBlockAt(at: BlockRelWorld, c: Offset, b: Block)(world: PlannedWorldChange): Unit = {
    val ch = at.offset(c)
    val bs = new BlockState(b)
    world.setBlock(ch, bs)
  }
}

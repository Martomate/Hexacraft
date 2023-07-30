package hexacraft.world

import hexacraft.renderer.PixelArray
import hexacraft.world.block.{BlockLoader, BlockSpec}

class FakeBlockLoader extends BlockLoader:
  override def reloadAllBlockTextures(): Seq[PixelArray] = Seq()

  override def loadBlockType(spec: BlockSpec): IndexedSeq[Int] = IndexedSeq.fill(8)(0)

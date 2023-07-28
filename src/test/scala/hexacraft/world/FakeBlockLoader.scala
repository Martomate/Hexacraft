package hexacraft.world

import hexacraft.renderer.TextureToLoad
import hexacraft.world.block.{BlockLoader, BlockSpec}

class FakeBlockLoader extends BlockLoader:
  override def reloadAllBlockTextures(): Seq[TextureToLoad] = Seq()

  override def loadBlockType(spec: BlockSpec): IndexedSeq[Int] = IndexedSeq.fill(8)(0)

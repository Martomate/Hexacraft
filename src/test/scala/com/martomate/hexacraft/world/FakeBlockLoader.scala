package com.martomate.hexacraft.world

import com.martomate.hexacraft.world.block.{BlockLoader, BlockSpec}

class FakeBlockLoader extends BlockLoader:
  override def loadBlockType(spec: BlockSpec): IndexedSeq[Int] = IndexedSeq.fill(8)(0)

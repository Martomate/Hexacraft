package com.martomate.hexacraft.world

import com.martomate.hexacraft.world.block.BlockLoader

class FakeBlockLoader extends BlockLoader:
  override def loadBlockType(name: String): IndexedSeq[Int] = IndexedSeq.fill(8)(0)

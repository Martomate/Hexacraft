package com.martomate.hexacraft.world

import java.util.Observable

import com.martomate.hexacraft.block.{Block, Blocks}

class Inventory extends Observable {
  private val slots = Seq(Blocks.Dirt, Blocks.Grass, Blocks.Sand, Blocks.Stone, Blocks.Water) ++ Seq.fill(4)(Blocks.Air)

  def apply(idx: Int): Block = slots(idx)

  def setHasChanged(): Unit = {
    setChanged()
    notifyObservers()
  }
}

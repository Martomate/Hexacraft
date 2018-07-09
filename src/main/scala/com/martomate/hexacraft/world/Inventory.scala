package com.martomate.hexacraft.world

import java.util.Observable

import com.martomate.hexacraft.block.Block

class Inventory extends Observable {
  private val slots = Seq(Block.Dirt, Block.Grass, Block.Sand, Block.Stone, Block.Water) ++ Seq.fill(4)(Block.Air)

  def apply(idx: Int): Block = slots(idx)

  def setHasChanged(): Unit = {
    setChanged()
    notifyObservers()
  }
}

package com.martomate.hexacraft.world.player

import java.util.Observable

import com.martomate.hexacraft.world.block.{Block, Blocks}

class Inventory extends Observable {
  private val slots = Seq(Blocks.Dirt, Blocks.Grass, Blocks.Sand, Blocks.Stone, Blocks.Water, Blocks.Log, Blocks.Leaves, Blocks.Planks, Blocks.BirchLog)

  def apply(idx: Int): Block = slots(idx)

  def setHasChanged(): Unit = {
    setChanged()
    notifyObservers()
  }
}

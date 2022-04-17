package com.martomate.hexacraft.world.player

import com.martomate.hexacraft.world.block.{Block, Blocks}

import scala.collection.mutable.ArrayBuffer

trait InventoryListener {
  def onInventoryChanged(): Unit
}

class Inventory {
  private val listeners = ArrayBuffer.empty[InventoryListener]
  def addListener(listener: InventoryListener): Unit = listeners += listener

  private val slots = Seq(Blocks.Dirt, Blocks.Grass, Blocks.Sand, Blocks.Stone, Blocks.Water, Blocks.Log, Blocks.Leaves, Blocks.Planks, Blocks.BirchLog)

  def apply(idx: Int): Block = slots(idx)

  def setHasChanged(): Unit = {
    for (listener <- listeners) listener.onInventoryChanged()
  }
}

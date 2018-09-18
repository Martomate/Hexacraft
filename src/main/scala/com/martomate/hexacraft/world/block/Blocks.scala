package com.martomate.hexacraft.world.block

object Blocks {
  private val factory: BlockFactory = new BlockFactory

  val Air     = factory("air")
  val Stone   = factory("stone")
  val Grass   = factory("grass")
  val Dirt    = factory("dirt")
  val Sand    = factory("sand")
  val Water   = factory("water")

  def init(): Unit = {
  }
}

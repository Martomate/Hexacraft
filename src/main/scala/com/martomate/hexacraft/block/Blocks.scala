package com.martomate.hexacraft.block

object Blocks {
  val Air: Block= BlockAir
  val Stone     = new Block(1, "stone", "Stone")
  val Grass     = new Block(2, "grass", "Grass")
  val Dirt      = new Block(3, "dirt", "Dirt")
  val Sand      = new Block(4, "sand", "Sand") with EmittingLight
  val Water     = new BlockFluid(5, "water", "Water")

  def init(): Unit = {
  }
}

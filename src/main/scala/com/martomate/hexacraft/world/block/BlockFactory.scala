package com.martomate.hexacraft.world.block

class BlockFactory {
  def apply(name: String): Block = name match {
    case "air"    => BlockAir
    case "stone"  => new Block(1, "stone", "Stone")
    case "grass"  => new Block(2, "grass", "Grass")
    case "dirt"   => new Block(3, "dirt", "Dirt")
    case "sand"   => new Block(4, "sand", "Sand") with EmittingLight
    case "water"  => new Block(5, "water", "Water")
  }
}

package com.martomate.hexacraft.world.block

import com.martomate.hexacraft.world.block.fluid.BlockFluid

class BlockFactory {
  def apply(name: String): Block = name match {
    case "air"    => BlockAir
    case "stone"  => new Block(1, "stone", "Stone")
    case "grass"  => new Block(2, "grass", "Grass")
    case "dirt"   => new Block(3, "dirt", "Dirt")
    case "sand"   => new Block(4, "sand", "Sand") with EmittingLight
    case "water"  => new BlockFluid(5, "water", "Water")
    case "log"    => new Block(6, "log", "Log")
    case "leaves" => new Block(7, "leaves", "Leaves")
    case _        => new Block(-128, "unknown", "Unknown")
  }
}

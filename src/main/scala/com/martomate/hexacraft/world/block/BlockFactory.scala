package com.martomate.hexacraft.world.block

import com.martomate.hexacraft.world.block.fluid.BlockFluid

class BlockFactory(using BlockLoader) {
  def apply(name: String): Block = name match {
    case "air"          => new BlockAir
    case "stone"        => new Block(1, "stone", "Stone")
    case "grass"        => new Block(2, "grass", "Grass")
    case "dirt"         => new Block(3, "dirt", "Dirt")
    case "sand"         => new Block(4, "sand", "Sand") with EmittingLight
    case "water"        => new BlockFluid(5, "water", "Water")
    case "log"          => new Block(6, "log", "Log")
    case "leaves"       => new Block(7, "leaves", "Leaves")
    case "planks"       => new Block(8, "planks", "Planks")
    case "log_birch"    => new Block(9, "log_birch", "Birch log")
    case "leaves_birch" => new Block(10, "leaves_birch", "Birch leaves")
    case _              => new Block(-128, "unknown", "Unknown")
  }
}

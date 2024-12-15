package hexacraft.client

import hexacraft.world.block.{Block, BlockSpec}
import hexacraft.world.block.BlockSpec.{Offsets, Textures}

object BlockSpecs {
  def default = Block.all.filter(_.canBeRendered).map(b => (b.name, specByBlockName(b.name))).toMap

  private def specByBlockName(name: String): BlockSpec = name match {
    case "stone" => BlockSpec(Textures.basic("stoneSide").withTop("stoneTop").withBottom("stoneTop"))
    case "grass" => BlockSpec(Textures.basic("grassSide").withTop("grassTop").withBottom("dirt"))
    case "dirt"  => BlockSpec(Textures.basic("dirt"))
    case "sand"  => BlockSpec(Textures.basic("sand"))
    case "water" => BlockSpec(Textures.basic("water"))
    case "log" =>
      BlockSpec(
        Textures
          .basic("logSide")
          .withTop("log", Offsets(0, 1, 2, 0, 1, 2))
          .withBottom("log", Offsets(0, 1, 2, 0, 1, 2))
      )
    case "leaves" => BlockSpec(Textures.basic("leaves"))
    case "planks" =>
      BlockSpec(
        Textures
          .basic("planks_side")
          .withTop("planks_top", Offsets(0, 1, 0, 1, 0, 1))
          .withBottom("planks_top", Offsets(0, 1, 0, 1, 0, 1))
      )
    case "log_birch" =>
      BlockSpec(
        Textures
          .basic("logSide_birch")
          .withTop("log_birch", Offsets(0, 1, 2, 0, 1, 2))
          .withBottom("log_birch", Offsets(0, 1, 2, 0, 1, 2))
      )
    case "leaves_birch" => BlockSpec(Textures.basic("leaves_birch"))
    case "tnt"          => BlockSpec(Textures.basic("tnt").withTop("tnt_top").withBottom("tnt_top"))
    case "glass"        => BlockSpec(Textures.basic("glass"))
  }
}

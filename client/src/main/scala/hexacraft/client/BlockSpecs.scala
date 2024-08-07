package hexacraft.client

import hexacraft.world.block.BlockSpec
import hexacraft.world.block.BlockSpec.{Offsets, Textures}

object BlockSpecs {
  def default = Map(
    "stone" -> BlockSpec(Textures.basic("stoneSide").withTop("stoneTop").withBottom("stoneTop")),
    "grass" -> BlockSpec(Textures.basic("grassSide").withTop("grassTop").withBottom("dirt")),
    "dirt" -> BlockSpec(Textures.basic("dirt")),
    "sand" -> BlockSpec(Textures.basic("sand")),
    "water" -> BlockSpec(Textures.basic("water")),
    "log" -> BlockSpec(
      Textures
        .basic("logSide")
        .withTop("log", Offsets(0, 1, 2, 0, 1, 2))
        .withBottom("log", Offsets(0, 1, 2, 0, 1, 2))
    ),
    "leaves" -> BlockSpec(Textures.basic("leaves")),
    "planks" -> BlockSpec(
      Textures
        .basic("planks_side")
        .withTop("planks_top", Offsets(0, 1, 0, 1, 0, 1))
        .withBottom("planks_top", Offsets(0, 1, 0, 1, 0, 1))
    ),
    "log_birch" -> BlockSpec(
      Textures
        .basic("logSide_birch")
        .withTop("log_birch", Offsets(0, 1, 2, 0, 1, 2))
        .withBottom("log_birch", Offsets(0, 1, 2, 0, 1, 2))
    ),
    "leaves_birch" -> BlockSpec(Textures.basic("leaves_birch")),
    "tnt" -> BlockSpec(Textures.basic("tnt").withTop("tnt_top").withBottom("tnt_top"))
  )
}

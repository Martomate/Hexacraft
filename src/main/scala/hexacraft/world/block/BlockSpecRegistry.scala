package hexacraft.world.block

import hexacraft.infra.fs.BlockTextureMapping

import scala.collection.mutable

class BlockSpecRegistry {
  private val textures = mutable.HashMap.empty[String, IndexedSeq[Int]]

  def register(specs: (String, BlockSpec)*)(textureMapping: BlockTextureMapping): Unit =
    for (name, spec) <- specs do textures(name) = textureMapping.textureArrayIndices(spec)

  def textureIndex(name: String, side: Int): Int = textures(name)(side)
}

object BlockSpecRegistry {
  def load(textureMapping: BlockTextureMapping): BlockSpecRegistry =
    import BlockSpec.*

    val specs = new BlockSpecRegistry

    specs.register(
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
      "leaves_birch" -> BlockSpec(Textures.basic("leaves_birch"))
    )(textureMapping)

    specs
}

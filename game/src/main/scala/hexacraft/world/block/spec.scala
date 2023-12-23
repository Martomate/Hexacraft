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
      "leaves_birch" -> BlockSpec(Textures.basic("leaves_birch")),
      "tnt" -> BlockSpec(Textures.basic("tnt").withTop("tnt_top").withBottom("tnt_top"))
    )(textureMapping)

    specs
}

case class BlockSpec(textures: BlockSpec.Textures)

object BlockSpec:
  case class Textures(
      sides: IndexedSeq[String],
      top: String,
      bottom: String,
      topOffsets: Offsets,
      bottomOffsets: Offsets
  ):
    def indices(texIdxMap: Map[String, Int]): IndexedSeq[Int] =
      val topIdx = texIdxMap(top) | topOffsets.packed << 12
      val bottomIdx = texIdxMap(bottom) | bottomOffsets.packed << 12
      val sidesIdx = sides.map(s => texIdxMap(s))
      topIdx +: bottomIdx +: sidesIdx

    def withTop(top: String, offsets: Offsets = Offsets.default): Textures =
      copy(top = top, topOffsets = offsets)
    def withBottom(bottom: String, offsets: Offsets = Offsets.default): Textures =
      copy(bottom = bottom, bottomOffsets = offsets)

  object Textures:
    def basic(all: String): Textures =
      Textures(IndexedSeq.fill(6)(all), all, all, Offsets.default, Offsets.default)

  case class Offsets(off0: Int, off1: Int, off2: Int, off3: Int, off4: Int, off5: Int):
    require(off0 == 0) // not allowed

    def packed: Int = off1 << 16 | off2 << 12 | off3 << 8 | off4 << 4 | off5

  object Offsets:
    def default: Offsets = Offsets(0, 0, 0, 0, 0, 0)

package hexacraft.world.block

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

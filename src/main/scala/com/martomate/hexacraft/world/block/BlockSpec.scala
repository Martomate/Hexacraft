package com.martomate.hexacraft.world.block

import com.eclipsesource.json.JsonObject

case class BlockSpec(textures: BlockSpec.Textures)

object BlockSpec:
  def fromJson(base: JsonObject): BlockSpec =
    val textures = base.get("textures").asObject()
    BlockSpec(Textures.fromJson(textures))

  case class Textures(
      all: Option[String],
      side: Option[String],
      sides: IndexedSeq[Option[String]],
      top: Option[String],
      bottom: Option[String],
      topOffsets: Textures.Offsets,
      bottomOffsets: Textures.Offsets
  ):
    def indices(texIdxMap: Map[String, Int]): IndexedSeq[Int] =
      val topIdx = texIdxMap(top.orElse(all).orNull) | topOffsets.packed << 12
      val bottomIdx = texIdxMap(bottom.orElse(all).orNull) | bottomOffsets.packed << 12
      val sidesIdx = sides.map(s => texIdxMap(s.orElse(side).orElse(all).orNull))
      topIdx +: bottomIdx +: sidesIdx

  object Textures:
    def fromJson(textures: JsonObject): Textures = {
      val all = Option(textures.get("all")).map(_.asString)
      val side = Option(textures.get("side")).map(_.asString)
      val top = Option(textures.get("top")).map(_.asString)
      val bottom = Option(textures.get("bottom")).map(_.asString)

      val sides =
        for i <- 2 until 8
        yield Option(textures.get(s"side$i")).map(_.asString)

      val topOffsets = Option(textures.get("topOffsets")) match
        case Some(v) => Offsets.fromJson(v.asObject)
        case None    => Offsets.default

      val bottomOffsets = Option(textures.get("bottomOffsets")) match
        case Some(v) => Offsets.fromJson(v.asObject)
        case None    => Offsets.default

      Textures(all, side, sides, top, bottom, topOffsets, bottomOffsets)
    }

    case class Offsets(off: IndexedSeq[Int]):
      require(off.size == 6)

      def packed: Int =
        if off(0) != 0
        then 0 // not allowed
        else off(1) << 16 | off(2) << 12 | off(3) << 8 | off(4) << 4 | off(5)

    object Offsets:
      def default: Offsets = Offsets(IndexedSeq.fill(6)(0))

      def fromJson(obj: JsonObject): Offsets =
        Offsets(
          IndexedSeq(
            obj.getInt("0", 0),
            obj.getInt("1", 0),
            obj.getInt("2", 0),
            obj.getInt("3", 0),
            obj.getInt("4", 0),
            obj.getInt("5", 0)
          )
        )

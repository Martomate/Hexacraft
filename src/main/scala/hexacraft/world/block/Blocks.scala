package hexacraft.world.block

import hexacraft.world.block.fluid.BlockFluid

object Blocks:
  private var _instance: Blocks = _
  def instance: Blocks = _instance

class Blocks(using blockLoader: BlockLoader):
  Blocks._instance = this
  val Air = new BlockAir
  val Stone = new Block(1, "stone", "Stone")
  val Grass = new Block(2, "grass", "Grass")
  val Dirt = new Block(3, "dirt", "Dirt")
  val Sand = new Block(4, "sand", "Sand") with EmittingLight
  val Water = new BlockFluid(5, "water", "Water")
  val Log = new Block(6, "log", "Log")
  val Leaves = new Block(7, "leaves", "Leaves")
  val Planks = new Block(8, "planks", "Planks")
  val BirchLog = new Block(9, "log_birch", "Birch log")
  val BirchLeaves = new Block(10, "leaves_birch", "Birch leaves")

  val textures: Map[String, IndexedSeq[Int]] =
    import BlockSpec.*
    Map(
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
    ).view.mapValues(blockLoader.loadBlockType).toMap

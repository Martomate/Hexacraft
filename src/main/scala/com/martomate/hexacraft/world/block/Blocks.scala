package com.martomate.hexacraft.world.block

object Blocks {
  private var _instance: Blocks = _
  def instance: Blocks = _instance
}

class Blocks(using factory: BlockFactory):
  Blocks._instance = this
  val Air = factory("air")
  val Stone = factory("stone")
  val Grass = factory("grass")
  val Dirt = factory("dirt")
  val Sand = factory("sand")
  val Water = factory("water")
  val Log = factory("log")
  val BirchLog = factory("log_birch")
  val Leaves = factory("leaves")
  val BirchLeaves = factory("leaves_birch")
  val Planks = factory("planks")

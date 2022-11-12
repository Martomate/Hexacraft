package com.martomate.hexacraft.world.gen.planner

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.BlocksInWorld
import com.martomate.hexacraft.world.block.Blocks
import com.martomate.hexacraft.world.chunk.{Chunk, ChunkColumn}
import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, ChunkRelWorld}
import com.martomate.hexacraft.world.gen.feature.tree.{
  GenTree,
  HugeTreeGenStrategy,
  ShortTreeGenStrategy,
  TallTreeGenStrategy
}
import com.martomate.hexacraft.world.gen.{PlannedChunkChange, PlannedWorldChange}

import scala.collection.mutable
import scala.util.Random

class TreePlanner(world: BlocksInWorld, mainSeed: Long)(using cylSize: CylinderSize, Blocks: Blocks)
    extends WorldFeaturePlanner {
  private val plannedChanges: mutable.Map[ChunkRelWorld, PlannedChunkChange] = mutable.Map.empty
  private val chunksPlanned: mutable.Set[ChunkRelWorld] = mutable.Set.empty

  private val maxTreesPerChunk = 5

  override def decorate(chunk: Chunk): Unit = {
    plannedChanges.remove(chunk.coords).foreach(ch => ch.applyChanges(chunk))
  }

  def treeLocations(coords: ChunkRelWorld): Seq[(Int, Int)] = {
    val rand = new Random(mainSeed ^ coords.value)
    val count = rand.nextInt(maxTreesPerChunk + 1)

    for (_ <- 0 until count) yield {
      val cx = rand.nextInt(16)
      val cz = rand.nextInt(16)
      (cx, cz)
    }
  }

  def plan(coords: ChunkRelWorld): Unit = {
    if (!chunksPlanned(coords)) {
      val column = world.provideColumn(coords.getColumnRelWorld)
      val locations = treeLocations(coords)
      for ((cx, cz) <- locations) {
        attemptTreeGenerationAt(coords, column, cx, cz, locations.size == 1)
      }

      chunksPlanned(coords) = true
    }
  }

  private def attemptTreeGenerationAt(
      coords: ChunkRelWorld,
      column: ChunkColumn,
      cx: Int,
      cz: Int,
      allowBig: Boolean
  ): Unit = {
    val yy = column.generatedHeightMap(cx)(cz)
    if (yy >= coords.Y * 16 && yy < (coords.Y + 1) * 16) {
      generateTree(coords, cx, cz, yy, allowBig)
    }
  }

  private def generateTree(
      coords: ChunkRelWorld,
      cx: Int,
      cz: Int,
      yy: Short,
      allowBig: Boolean
  ): Unit = {
    val rand = new Random(mainSeed ^ coords.value + 836538746785L * (cx * 16 + cz + 387L))

    // short and tall trees can be birches, but the huge ones cannot
    val isBirchTree = rand.nextDouble() < 0.1
    val logBlock = if (isBirchTree) Blocks.BirchLog else Blocks.Log
    val leavesBlock = if (isBirchTree) Blocks.BirchLeaves else Blocks.Leaves

    val choice = rand.nextDouble()
    val treeGenStrategy = {
      if (allowBig && choice < 0.05) new HugeTreeGenStrategy(24, 1, rand)
      else if (choice < 0.3) new TallTreeGenStrategy(16, rand)(logBlock, leavesBlock)
      else new ShortTreeGenStrategy(logBlock, leavesBlock)
    }

    val tree =
      new GenTree(BlockRelWorld(coords.X * 16 + cx, yy, coords.Z * 16 + cz), treeGenStrategy)
        .generate()
    generateChanges(tree)
  }

  private def generateChanges(tree: PlannedWorldChange): Unit = {
    for ((c, ch) <- tree.chunkChanges) {
      appendChange(c, ch)
    }
  }

  private def appendChange(c: ChunkRelWorld, ch: PlannedChunkChange): Unit = {
    plannedChanges.get(c) match {
      case Some(old) =>
        old.merge(ch)
      case None =>
        plannedChanges(c) = ch
    }
  }
}

package com.martomate.hexacraft.world.gen.planner

import com.martomate.hexacraft.world.chunk.IChunk
import com.martomate.hexacraft.world.column.ChunkColumn
import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, ChunkRelWorld}
import com.martomate.hexacraft.world.gen.feature.GenTree
import com.martomate.hexacraft.world.gen.{PlannedChunkChange, PlannedWorldChange}
import com.martomate.hexacraft.world.worldlike.IWorld

import scala.collection.mutable
import scala.util.Random

class TreePlanner(world: IWorld) extends WorldFeaturePlanner {
  private val plannedChanges: mutable.Map[ChunkRelWorld, PlannedChunkChange] = mutable.Map.empty
  private val chunksPlanned: mutable.Set[ChunkRelWorld] = mutable.Set.empty

  private val maxTreesPerChunk = 5

  override def decorate(chunk: IChunk): Unit = {
    plannedChanges.remove(chunk.coords).foreach(ch => ch.applyChanges(chunk))
  }

  def treeLocations(coords: ChunkRelWorld): Seq[(Int, Int)] = {
    val rand = new Random(world.worldSettings.gen.seed ^ coords.value)
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
      for ((cx, cz) <- treeLocations(coords)) {
        attemptTreeGenerationAt(coords, column, cx, cz)
      }

      chunksPlanned(coords) = true
    }
  }

  private def attemptTreeGenerationAt(coords: ChunkRelWorld, column: ChunkColumn, cx: Int, cz: Int): Unit = {
    val yy = column.generatedHeightMap(cx)(cz)
    if (yy >= coords.Y * 16 && yy < (coords.Y + 1) * 16) {
      generateTree(coords, cx, cz, yy)
    }
  }

  private def generateTree(coords: ChunkRelWorld, cx: Int, cz: Int, yy: Short): Unit = {
    import coords.cylSize.impl
    val tree = new GenTree(BlockRelWorld(coords.X * 16 + cx, yy, coords.Z * 16 + cz)).generate()
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

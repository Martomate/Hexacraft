package com.martomate.hexacraft.world.gen.planner

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.BlocksInWorld
import com.martomate.hexacraft.world.block.Blocks
import com.martomate.hexacraft.world.chunk.Chunk
import com.martomate.hexacraft.world.coord.fp.BlockCoords
import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, ChunkRelWorld}
import com.martomate.hexacraft.world.entity.{EntityFactory, EntityModelLoader}
import com.martomate.hexacraft.world.entity.sheep.SheepEntity
import com.martomate.hexacraft.world.gen.PlannedEntitySpawn

import scala.collection.mutable
import scala.util.Random

class SheepPlanner(world: BlocksInWorld, sheepFactory: EntityFactory[?], mainSeed: Long)(using
    EntityModelLoader,
    CylinderSize,
    Blocks
) extends WorldFeaturePlanner {
  private val plannedSheep: mutable.Map[ChunkRelWorld, PlannedEntitySpawn] = mutable.Map.empty
  private val chunksPlanned: mutable.Set[ChunkRelWorld] = mutable.Set.empty

  private val maxSheepPerGroup = 7

  override def decorate(chunk: Chunk): Unit = {
    plannedSheep.get(chunk.coords).foreach(_.spawnEntities(chunk))
  }

  override def plan(coords: ChunkRelWorld): Unit = {
    if (!chunksPlanned(coords)) {
      val rand = new Random(mainSeed ^ coords.value + 364453868)
      if (rand.nextDouble() < 0.01) {
        val thePlan = new PlannedEntitySpawn
        val count = rand.nextInt(maxSheepPerGroup) + 1
        for (_ <- 0 until count) {
          val column = world.provideColumn(coords.getColumnRelWorld)
          val cx = rand.nextInt(16)
          val cz = rand.nextInt(16)
          val y = column.heightMap(cx, cz)
          if (y >= coords.Y * 16 && y < (coords.Y + 1) * 16) {
            val sheepStartPos = BlockCoords(BlockRelWorld(cx, y & 15, cz, coords)).toCylCoords.offset(0, 0.001f, 0)
            val sheep = sheepFactory.atStartPos(sheepStartPos)
            thePlan.addEntity(sheep)
          }
        }

        plannedSheep(coords) = thePlan
      }
      chunksPlanned += coords
    }
  }
}

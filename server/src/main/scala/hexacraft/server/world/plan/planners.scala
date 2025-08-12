package hexacraft.server.world.plan

import hexacraft.server.world.plan.tree.{
  HugeTreeGenStrategy,
  RainforestTreeGenStrategy,
  ShortTreeGenStrategy,
  TallTreeGenStrategy
}
import hexacraft.util.{LongSet, Loop}
import hexacraft.world.{BlocksInWorldExtended, CylinderSize}
import hexacraft.world.block.{Block, BlockState}
import hexacraft.world.chunk.{Biome, Chunk, ChunkColumnTerrain, LocalBlockState}
import hexacraft.world.coord.{BlockCoords, BlockRelWorld, ChunkRelWorld, CylCoords}
import hexacraft.world.entity.Entity

import org.joml.Vector2i

import scala.collection.mutable
import scala.util.Random

trait WorldFeaturePlanner {
  def decorate(chunkCoords: ChunkRelWorld, chunk: Chunk): Unit
  def plan(chunkCoords: ChunkRelWorld): Unit
}

private class PlannedWorldChange {
  private val changes: mutable.Map[ChunkRelWorld, mutable.Buffer[LocalBlockState]] = mutable.Map.empty

  def setBlock(coords: BlockRelWorld, block: BlockState): Unit = {
    changes
      .getOrElseUpdate(coords.getChunkRelWorld, mutable.Buffer.empty)
      .append(LocalBlockState(coords.getBlockRelChunk, block))
  }

  def chunkChanges: Map[ChunkRelWorld, Seq[LocalBlockState]] = changes.view.mapValues(_.toSeq).toMap
}

private object PlannedWorldChange {
  def from(blocks: Seq[(BlockRelWorld, BlockState)]): PlannedWorldChange = {
    val worldChange = new PlannedWorldChange
    for (c, b) <- blocks do {
      worldChange.setBlock(c, b)
    }
    worldChange
  }
}

private class WoodChoice(val log: Block, val leaves: Block)

class TreePlanner(world: BlocksInWorldExtended, mainSeed: Long)(using cylSize: CylinderSize)
    extends WorldFeaturePlanner {
  private val plannedChanges: mutable.LongMap[mutable.ArrayBuffer[LocalBlockState]] =
    mutable.LongMap.empty

  private val maxTreesPerChunk = 5

  override def decorate(chunkCoords: ChunkRelWorld, chunk: Chunk): Unit = {
    plannedChanges.remove(chunkCoords.value) match {
      case Some(changes) =>
        Loop.array(changes) { case LocalBlockState(c, b) =>
          chunk.setBlock(c, b)
        }
      case None =>
    }
  }

  private def treeLocations(coords: ChunkRelWorld, biome: Biome): Array[Vector2i] = {
    val rand = new Random(mainSeed ^ coords.value)
    val count = if biome == Biome.Rainforest then {
      rand.nextInt(maxTreesPerChunk + 1) + 4
    } else {
      rand.nextInt(maxTreesPerChunk + 1)
    }

    Array.fill(count) {
      val cx = rand.nextInt(16)
      val cz = rand.nextInt(16)
      new Vector2i(cx, cz)
    }
  }

  def plan(coords: ChunkRelWorld): Unit = {
    val column = world.provideColumn(coords.getColumnRelWorld)
    val terrainHeight = column.originalTerrainHeight

    val locations = treeLocations(coords, column.biome(8, 8))
    val allowBig = locations.length == 1

    Loop.array(locations) { loc =>
      val cx = loc.x
      val cz = loc.y

      val yy = terrainHeight.getHeight(cx, cz)
      if yy >= coords.Y.toInt * 16 && yy < (coords.Y.toInt + 1) * 16 then {
        column.biome(cx, cz) match {
          case Biome.Ocean  =>
          case Biome.Desert =>
          case biome @ Biome.Rainforest =>
            generateTree(coords, cx, cz, yy, false, biome)
          case biome =>
            generateTree(coords, cx, cz, yy, allowBig, biome)
        }
      }
    }
  }

  private def generateTree(
      coords: ChunkRelWorld,
      cx: Int,
      cz: Int,
      yy: Short,
      allowBig: Boolean,
      biome: Biome
  ): Unit = {
    val rand = new Random(mainSeed ^ coords.value + 836538746785L * (cx * 16 + cz + 387L))

    val birchWood = WoodChoice(Block.BirchLog, Block.BirchLeaves)
    val oakWood = WoodChoice(Block.OakLog, Block.OakLeaves)

    // short and tall trees can be birches, but the huge ones cannot
    val choice = rand.nextDouble()
    val treeGenStrategy =
      if allowBig && choice < 0.05 then {
        new HugeTreeGenStrategy(oakWood.log, oakWood.leaves)
      } else {
        val useBirch = rand.nextDouble() < 0.1 && biome != Biome.Rainforest
        val woodChoice = if useBirch then birchWood else oakWood

        biome match {
          case Biome.Rainforest =>
            val height = (rand.nextDouble() * 12 + 12).toInt
            new RainforestTreeGenStrategy(height)(woodChoice.log, woodChoice.leaves)
          case _ =>
            if choice < 0.3 then {
              new TallTreeGenStrategy(16)(woodChoice.log, woodChoice.leaves)
            } else {
              new ShortTreeGenStrategy(woodChoice.log, woodChoice.leaves)
            }
        }
      }

    val groundCoords = BlockRelWorld(coords.X.toInt * 16 + cx, yy, coords.Z.toInt * 16 + cz)
    val tree = PlannedWorldChange.from(
      for (c, b) <- treeGenStrategy.blocks(rand)
      yield (groundCoords.offset(c), new BlockState(b))
    )
    generateChanges(tree)
  }

  private def generateChanges(tree: PlannedWorldChange): Unit = {
    for (c, ch) <- tree.chunkChanges do {
      plannedChanges.getOrElseUpdate(c.value, mutable.ArrayBuffer.empty).appendAll(ch)
    }
  }
}

class EntityGroupPlanner(world: BlocksInWorldExtended, entityFactory: CylCoords => Entity, mainSeed: Long)(using
    CylinderSize
) extends WorldFeaturePlanner {

  private val plannedEntities: mutable.LongMap[IndexedSeq[Entity]] = mutable.LongMap.empty

  private val maxEntitiesPerGroup = 7

  override def decorate(chunkCoords: ChunkRelWorld, chunk: Chunk): Unit = {
    plannedEntities.get(chunkCoords.value) match {
      case Some(entities) =>
        Loop.array(entities) { entity =>
          chunk.addEntity(entity)
        }
      case None =>
    }
  }

  override def plan(coords: ChunkRelWorld): Unit = {
    val column = world.provideColumn(coords.getColumnRelWorld)
    column.biome(8, 8) match {
      case Biome.Ocean  =>
      case Biome.Desert =>
      case _ =>
        val rand = new Random(mainSeed ^ coords.value + 364453868)
        if rand.nextDouble() < 0.01 then {
          plannedEntities(coords.value) = makePlan(rand, coords, column)
        }
    }
  }

  private def makePlan(rand: Random, coords: ChunkRelWorld, column: ChunkColumnTerrain): IndexedSeq[Entity] = {
    val thePlan = mutable.Buffer.empty[Entity]
    val count = rand.nextInt(maxEntitiesPerGroup) + 1
    for _ <- 0 until count do {
      val cx = rand.nextInt(16)
      val cz = rand.nextInt(16)
      val y = column.terrainHeight.getHeight(cx, cz)
      if y >= coords.Y.toInt * 16 && y < (coords.Y.toInt + 1) * 16 then {
        val groundCoords = BlockCoords(BlockRelWorld(cx, y & 15, cz, coords)).toCylCoords
        val entityStartPos = groundCoords.offset(0, 0.001f, 0)
        val entity = entityFactory(entityStartPos)
        thePlan += entity
      }
    }
    thePlan.toIndexedSeq
  }
}

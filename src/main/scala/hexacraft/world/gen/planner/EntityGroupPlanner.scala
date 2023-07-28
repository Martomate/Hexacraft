package hexacraft.world.gen.planner

import hexacraft.world.{BlocksInWorld, CylinderSize}
import hexacraft.world.block.Blocks
import hexacraft.world.chunk.Chunk
import hexacraft.world.coord.fp.BlockCoords
import hexacraft.world.coord.integer.{BlockRelWorld, ChunkRelWorld}
import hexacraft.world.entity.{Entity, EntityFactory}
import hexacraft.world.entity.sheep.SheepEntity

import scala.collection.mutable
import scala.util.Random

class EntityGroupPlanner(world: BlocksInWorld, entityFactory: EntityFactory, mainSeed: Long)(using
    CylinderSize,
    Blocks
) extends WorldFeaturePlanner:
  private val plannedEntities: mutable.Map[ChunkRelWorld, Seq[Entity]] = mutable.Map.empty
  private val chunksPlanned: mutable.Set[ChunkRelWorld] = mutable.Set.empty

  private val maxEntitiesPerGroup = 7

  override def decorate(chunk: Chunk): Unit =
    for
      entities <- plannedEntities.get(chunk.coords)
      entity <- entities
    do chunk.addEntity(entity)

  override def plan(coords: ChunkRelWorld): Unit =
    if !chunksPlanned(coords) then
      val rand = new Random(mainSeed ^ coords.value + 364453868)
      if rand.nextDouble() < 0.01 then plannedEntities(coords) = makePlan(rand, coords)
      chunksPlanned += coords

  private def makePlan(rand: Random, coords: ChunkRelWorld): Seq[Entity] =
    val thePlan = mutable.Buffer.empty[Entity]
    val count = rand.nextInt(maxEntitiesPerGroup) + 1
    for _ <- 0 until count do
      val column = world.provideColumn(coords.getColumnRelWorld)
      val cx = rand.nextInt(16)
      val cz = rand.nextInt(16)
      val y = column.terrainHeight(cx, cz)
      if y >= coords.Y.toInt * 16 && y < (coords.Y.toInt + 1) * 16 then
        val groundCoords = BlockCoords(BlockRelWorld(cx, y & 15, cz, coords)).toCylCoords
        val entityStartPos = groundCoords.offset(0, 0.001f, 0)
        val entity = entityFactory.atStartPos(entityStartPos)
        thePlan += entity
    thePlan.toSeq

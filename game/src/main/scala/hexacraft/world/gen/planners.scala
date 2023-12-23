package hexacraft.world.gen

import hexacraft.world.{BlocksInWorld, CylinderSize}
import hexacraft.world.block.{Block, BlockState}
import hexacraft.world.chunk.{Chunk, ChunkColumnTerrain}
import hexacraft.world.chunk.storage.LocalBlockState
import hexacraft.world.coord.fp.BlockCoords
import hexacraft.world.coord.integer.{BlockRelWorld, ChunkRelWorld}
import hexacraft.world.entity.{Entity, EntityFactory}
import hexacraft.world.gen.tree.{HugeTreeGenStrategy, ShortTreeGenStrategy, TallTreeGenStrategy}

import scala.collection.mutable
import scala.util.Random

trait WorldFeaturePlanner {
  def decorate(chunk: Chunk): Unit
  def plan(coords: ChunkRelWorld): Unit
}

class PlannedWorldChange:
  private val changes: mutable.Map[ChunkRelWorld, mutable.Buffer[LocalBlockState]] = mutable.Map.empty

  def setBlock(coords: BlockRelWorld, block: BlockState): Unit =
    changes
      .getOrElseUpdate(coords.getChunkRelWorld, mutable.Buffer.empty)
      .append(LocalBlockState(coords.getBlockRelChunk, block))

  def chunkChanges: Map[ChunkRelWorld, Seq[LocalBlockState]] = changes.view.mapValues(_.toSeq).toMap

object PlannedWorldChange:
  def from(blocks: Seq[(BlockRelWorld, BlockState)]): PlannedWorldChange =
    val worldChange = new PlannedWorldChange
    for (c, b) <- blocks do worldChange.setBlock(c, b)
    worldChange

class TreePlanner(world: BlocksInWorld, mainSeed: Long)(using cylSize: CylinderSize) extends WorldFeaturePlanner:
  private val plannedChanges: mutable.Map[ChunkRelWorld, mutable.Buffer[LocalBlockState]] = mutable.Map.empty
  private val chunksPlanned: mutable.Set[ChunkRelWorld] = mutable.Set.empty

  private val maxTreesPerChunk = 5

  override def decorate(chunk: Chunk): Unit =
    for
      ch <- plannedChanges.remove(chunk.coords)
      LocalBlockState(c, b) <- ch
    do chunk.setBlock(c, b)

  private def treeLocations(coords: ChunkRelWorld): Seq[(Int, Int)] =
    val rand = new Random(mainSeed ^ coords.value)
    val count = rand.nextInt(maxTreesPerChunk + 1)

    for _ <- 0 until count yield
      val cx = rand.nextInt(16)
      val cz = rand.nextInt(16)
      (cx, cz)

  def plan(coords: ChunkRelWorld): Unit =
    if !chunksPlanned(coords) then
      val column = world.provideColumn(coords.getColumnRelWorld)
      val locations = treeLocations(coords)
      for (cx, cz) <- locations do attemptTreeGenerationAt(coords, column, cx, cz, locations.size == 1)
      chunksPlanned(coords) = true

  private def attemptTreeGenerationAt(
      coords: ChunkRelWorld,
      column: ChunkColumnTerrain,
      cx: Int,
      cz: Int,
      allowBig: Boolean
  ): Unit =
    val yy = column.originalTerrainHeight(cx, cz)
    if yy >= coords.Y.toInt * 16 && yy < (coords.Y.toInt + 1) * 16 then generateTree(coords, cx, cz, yy, allowBig)

  private def generateTree(coords: ChunkRelWorld, cx: Int, cz: Int, yy: Short, allowBig: Boolean): Unit =
    val rand = new Random(mainSeed ^ coords.value + 836538746785L * (cx * 16 + cz + 387L))

    // short and tall trees can be birches, but the huge ones cannot
    val isBirchTree = rand.nextDouble() < 0.1
    val logBlock = if isBirchTree then Block.BirchLog else Block.Log
    val leavesBlock = if isBirchTree then Block.BirchLeaves else Block.Leaves

    val choice = rand.nextDouble()
    val treeGenStrategy =
      if allowBig && choice < 0.05
      then new HugeTreeGenStrategy(24, 1, rand)
      else if choice < 0.3
      then new TallTreeGenStrategy(16, rand)(logBlock, leavesBlock)
      else new ShortTreeGenStrategy(logBlock, leavesBlock)

    val groundCoords = BlockRelWorld(coords.X.toInt * 16 + cx, yy, coords.Z.toInt * 16 + cz)
    val tree = PlannedWorldChange.from(
      for (c, b) <- treeGenStrategy.blocks
      yield (groundCoords.offset(c), new BlockState(b))
    )
    generateChanges(tree)

  private def generateChanges(tree: PlannedWorldChange): Unit =
    for (c, ch) <- tree.chunkChanges do plannedChanges.getOrElseUpdate(c, mutable.Buffer.empty).appendAll(ch)

class EntityGroupPlanner(world: BlocksInWorld, entityFactory: EntityFactory, mainSeed: Long)(using
    CylinderSize
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

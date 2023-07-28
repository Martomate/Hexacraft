package hexacraft.world.gen.planner

import hexacraft.world.{BlocksInWorld, CylinderSize}
import hexacraft.world.block.{Blocks, BlockState}
import hexacraft.world.chunk.{Chunk, ChunkColumn, ChunkColumnTerrain}
import hexacraft.world.chunk.storage.LocalBlockState
import hexacraft.world.coord.integer.{BlockRelWorld, ChunkRelWorld}
import hexacraft.world.gen.PlannedWorldChange
import hexacraft.world.gen.feature.tree.{HugeTreeGenStrategy, ShortTreeGenStrategy, TallTreeGenStrategy}

import scala.collection.mutable
import scala.util.Random

class TreePlanner(world: BlocksInWorld, mainSeed: Long)(using cylSize: CylinderSize, Blocks: Blocks)
    extends WorldFeaturePlanner:
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
    val logBlock = if isBirchTree then Blocks.BirchLog else Blocks.Log
    val leavesBlock = if isBirchTree then Blocks.BirchLeaves else Blocks.Leaves

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

// TODO: fix bug where trees are not spawning anymore! (and maybe add a test for it..)

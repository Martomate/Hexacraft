package hexacraft.world.gen.planner

import hexacraft.world.chunk.Chunk
import hexacraft.world.coord.integer.ChunkRelWorld

trait WorldFeaturePlanner {
  def decorate(chunk: Chunk): Unit
  def plan(coords: ChunkRelWorld): Unit
}

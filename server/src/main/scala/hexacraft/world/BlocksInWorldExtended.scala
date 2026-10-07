package hexacraft.world

import hexacraft.world.chunk.ChunkColumnTerrain
import hexacraft.world.coord.ColumnRelWorld

trait BlocksInWorldExtended extends BlocksInWorld {
  def provideColumn(coords: ColumnRelWorld): ChunkColumnTerrain
}

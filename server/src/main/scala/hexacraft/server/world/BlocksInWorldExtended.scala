package hexacraft.server.world

import hexacraft.world.BlocksInWorld
import hexacraft.world.coord.ColumnRelWorld

trait BlocksInWorldExtended extends BlocksInWorld {
  def provideColumn(coords: ColumnRelWorld): ChunkColumnTerrain
}

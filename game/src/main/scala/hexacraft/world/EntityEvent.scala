package hexacraft.world

import hexacraft.world.coord.CylCoords

import com.martomate.nbt.Nbt

enum EntityEvent {
  case Spawned(data: Nbt.MapTag)
  case Despawned
  case Position(pos: CylCoords)
}

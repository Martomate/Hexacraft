package hexacraft.world

import hexacraft.nbt.Nbt
import hexacraft.world.coord.CylCoords

import org.joml.Vector3d

enum EntityEvent {
  case Spawned(data: Nbt.MapTag)
  case Despawned
  case Position(pos: CylCoords)
  case Rotation(v: Vector3d)
  case Velocity(v: Vector3d)
  case Flying(f: Boolean)
  case HeadDirection(d: Vector3d)
}

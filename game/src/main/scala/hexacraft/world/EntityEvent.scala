package hexacraft.world

import hexacraft.nbt.{Nbt, NbtDecoder, NbtEncoder}
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

object EntityEvent {
  given NbtEncoder[EntityEvent] with {
    override def encode(e: EntityEvent): Nbt.MapTag = {
      val name = e match {
        case EntityEvent.Spawned(_)       => "spawned"
        case EntityEvent.Despawned        => "despawned"
        case EntityEvent.Position(_)      => "position"
        case EntityEvent.Rotation(_)      => "rotation"
        case EntityEvent.Velocity(_)      => "velocity"
        case EntityEvent.Flying(_)        => "flying"
        case EntityEvent.HeadDirection(_) => "head_direction"
      }

      val extraFields: Seq[(String, Nbt)] = e match {
        case EntityEvent.Spawned(data)    => Seq("data" -> data)
        case EntityEvent.Despawned        => Seq()
        case EntityEvent.Position(pos)    => Seq("pos" -> Nbt.makeVectorTag(pos.toVector3d))
        case EntityEvent.Rotation(r)      => Seq("r" -> Nbt.makeVectorTag(r))
        case EntityEvent.Velocity(v)      => Seq("v" -> Nbt.makeVectorTag(v))
        case EntityEvent.Flying(f)        => Seq("f" -> Nbt.ByteTag(f))
        case EntityEvent.HeadDirection(d) => Seq("d" -> Nbt.makeVectorTag(d))
      }

      Nbt.makeMap(extraFields*).withField("type", Nbt.StringTag(name))
    }
  }

  given (using CylinderSize): NbtDecoder[EntityEvent] with {
    override def decode(eventNbt: Nbt.MapTag): Option[EntityEvent] = {
      eventNbt.getString("type").collect {
        case "spawned"        => EntityEvent.Spawned(eventNbt.getMap("data").get)
        case "despawned"      => EntityEvent.Despawned
        case "position"       => EntityEvent.Position(CylCoords(eventNbt.getMap("pos").get.setVector(new Vector3d)))
        case "rotation"       => EntityEvent.Rotation(eventNbt.getMap("r").get.setVector(new Vector3d))
        case "velocity"       => EntityEvent.Velocity(eventNbt.getMap("v").get.setVector(new Vector3d))
        case "flying"         => EntityEvent.Flying(eventNbt.getBoolean("f", false))
        case "head_direction" => EntityEvent.HeadDirection(eventNbt.getMap("d").get.setVector(new Vector3d))
      }
    }
  }
}

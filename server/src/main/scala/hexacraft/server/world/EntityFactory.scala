package hexacraft.server.world

import hexacraft.util.Result
import hexacraft.world.CylinderSize
import hexacraft.world.coord.CylCoords
import hexacraft.world.entity.{AiComponent, Entity, SimpleWalkAI}

import org.joml.Vector3d

import java.util.UUID

object EntityFactory {

  /** Creates a new entity of the given type, including any server-side components like AI. */
  def atStartPos(
      id: UUID,
      pos: CylCoords,
      entityType: String,
      rotation: Vector3d = new Vector3d
  )(using CylinderSize): Result[Entity, String] = {
    Entity.atStartPos(id, pos, entityType, rotation).map { entity =>
      entityType match {
        case "sheep" => entity.withComponent(AiComponent(SimpleWalkAI.create))
        case _       => entity
      }
    }
  }
}

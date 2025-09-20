package hexacraft.world.entity

import hexacraft.nbt.{Nbt, NbtDecoder, NbtEncoder}
import hexacraft.util.Result
import hexacraft.util.Result.{Err, Ok}
import hexacraft.world.{CylinderSize, HexBox}
import hexacraft.world.coord.CylCoords

import java.util.UUID

class Entity(val id: UUID, val typeName: String, private val components: Seq[EntityComponent] = Nil) {
  val transform: TransformComponent = components
    .find(_.isInstanceOf[TransformComponent])
    .map(_.asInstanceOf[TransformComponent])
    .orNull

  val motion: MotionComponent = components
    .find(_.isInstanceOf[MotionComponent])
    .map(_.asInstanceOf[MotionComponent])
    .orNull

  val headDirection: Option[HeadDirectionComponent] = components
    .find(_.isInstanceOf[HeadDirectionComponent])
    .map(_.asInstanceOf[HeadDirectionComponent])

  val boundingBox: HexBox = components
    .find(_.isInstanceOf[BoundsComponent])
    .map(_.asInstanceOf[BoundsComponent].bounds)
    .orNull

  val model: Option[EntityModel] = components
    .find(_.isInstanceOf[ModelComponent])
    .map(_.asInstanceOf[ModelComponent].model)

  val ai: Option[EntityAI] = components
    .find(_.isInstanceOf[AiComponent])
    .map(_.asInstanceOf[AiComponent].ai)
}

object Entity {
  def getNextId: UUID = UUID.randomUUID()

  def apply(id: UUID, typeName: String, components: Seq[EntityComponent]): Entity =
    new Entity(id, typeName, components)

  val playerBounds = new HexBox(0.2f, 0, 1.75f)
  private val sheepBounds = new HexBox(0.4f, 0, 0.75f)

  def atStartPos(id: UUID, pos: CylCoords, entityType: String)(using CylinderSize): Result[Entity, String] = {
    Nbt.decode[Entity](Nbt.makeMap("type" -> Nbt.StringTag(entityType), "id" -> Nbt.StringTag(id.toString))) match {
      case Some(e) =>
        e.transform.position = pos
        Ok(e)
      case None =>
        Err(s"Entity-type '$entityType' not found")
    }
  }

  given NbtEncoder[Entity] with {
    override def encode(e: Entity): Nbt.MapTag = {
      Nbt
        .makeMap(
          "type" -> Nbt.StringTag(e.typeName),
          "id" -> Nbt.StringTag(e.id.toString),
          "pos" -> Nbt.makeVectorTag(e.transform.position.toVector3d),
          "velocity" -> Nbt.makeVectorTag(e.motion.velocity),
          "rotation" -> Nbt.makeVectorTag(e.transform.rotation)
        )
        .withOptionalField("ai", e.ai.map(_.toNBT))
    }
  }

  given (using CylinderSize): NbtDecoder[Entity] with {
    override def decode(tag: Nbt.MapTag): Option[Entity] = {
      val id = tag.getString("id").map(UUID.fromString).getOrElse(UUID.randomUUID())
      val entType = tag.getString("type", "")

      entType match {
        case "player" =>
          val components = Seq(
            TransformComponent.fromNBT(tag),
            MotionComponent.fromNBT(tag),
            HeadDirectionComponent.fromNBT(tag),
            BoundsComponent(playerBounds),
            ModelComponent(PlayerEntityModel.create("player"))
          )
          Some(Entity(id, "player", components))

        case "sheep" =>
          val components = Seq(
            TransformComponent.fromNBT(tag),
            MotionComponent.fromNBT(tag),
            AiComponent.fromNBT(tag),
            BoundsComponent(sheepBounds),
            ModelComponent(SheepEntityModel.create("sheep"))
          )
          Some(Entity(id, "sheep", components))

        case _ => None
      }
    }
  }
}

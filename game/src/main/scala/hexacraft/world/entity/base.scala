package hexacraft.world.entity

import hexacraft.util.Result
import hexacraft.util.Result.{Err, Ok}
import hexacraft.world.{CylinderSize, HexBox}
import hexacraft.world.coord.CylCoords

import com.martomate.nbt.Nbt

object Entity {
  def apply(typeName: String, components: Seq[EntityComponent]): Entity = new Entity(typeName, components)
}

class Entity(val typeName: String, private val components: Seq[EntityComponent] = Nil) {
  val transform: TransformComponent = components
    .find(_.isInstanceOf[TransformComponent])
    .map(_.asInstanceOf[TransformComponent])
    .orNull

  val velocity: VelocityComponent = components
    .find(_.isInstanceOf[VelocityComponent])
    .map(_.asInstanceOf[VelocityComponent])
    .orNull

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

  def toNBT: Nbt.MapTag = {
    Nbt
      .makeMap(
        "type" -> Nbt.StringTag(typeName),
        "pos" -> Nbt.makeVectorTag(transform.position.toVector3d),
        "velocity" -> Nbt.makeVectorTag(velocity.velocity),
        "rotation" -> Nbt.makeVectorTag(transform.rotation)
      )
      .withOptionalField("ai", ai.map(_.toNBT))
  }
}

object EntityFactory {
  val playerBounds = new HexBox(0.2f, 0, 1.75f)
  private val sheepBounds = new HexBox(0.4f, 0, 0.75f)

  def atStartPos(pos: CylCoords, entityType: String)(using CylinderSize): Result[Entity, String] = {
    fromNbt(Nbt.makeMap("type" -> Nbt.StringTag(entityType))).map: e =>
      e.transform.position = pos
      e
  }

  def fromNbt(tag: Nbt.MapTag)(using CylinderSize): Result[Entity, String] = {
    val entType = tag.getString("type", "")

    entType match {
      case "player" =>
        val components = Seq(
          TransformComponent.fromNBT(tag),
          VelocityComponent.fromNBT(tag),
          AiComponent.fromNBT(tag),
          BoundsComponent(playerBounds),
          ModelComponent(PlayerEntityModel.create("player"))
        )
        Ok(Entity("player", components))

      case "sheep" =>
        val components = Seq(
          TransformComponent.fromNBT(tag),
          VelocityComponent.fromNBT(tag),
          AiComponent.fromNBT(tag),
          BoundsComponent(sheepBounds),
          ModelComponent(SheepEntityModel.create("sheep"))
        )
        Ok(Entity("sheep", components))

      case _ => Err(s"Entity-type '$entType' not found")
    }
  }
}

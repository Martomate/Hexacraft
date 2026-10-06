package hexacraft.world.entity

import hexacraft.nbt.{Nbt, NbtDecoder, NbtEncoder}
import hexacraft.util.Result
import hexacraft.util.Result.{Err, Ok}
import hexacraft.world.{CylinderSize, HexBox}
import hexacraft.world.coord.CylCoords

import org.joml.Vector3d

import java.util.UUID

class Entity(val id: UUID, val typeName: String, private val components: Seq[EntityComponent] = Nil) {
  val transform: TransformComponent =
    components.collectFirst { case c: TransformComponent => c }.get

  val motion: MotionComponent =
    components.collectFirst { case c: MotionComponent => c }.get

  val headDirection: Option[HeadDirectionComponent] =
    components.collectFirst { case c: HeadDirectionComponent => c }

  val boundingBox: HexBox =
    components.collectFirst { case c: BoundsComponent => c.bounds }.get

  val model: Option[EntityModel] =
    components.collectFirst { case c: ModelComponent => c.model }

  val mountedEntities: Seq[MountComponent] =
    components.collect { case c: MountComponent => c }

  val ai: Option[EntityAI] =
    components.collectFirst { case c: AiComponent => c.ai }

  def withComponent(component: EntityComponent): Entity =
    new Entity(id, typeName, components :+ component)

  def withoutComponents(predicate: EntityComponent => Boolean): Entity =
    new Entity(id, typeName, components.filterNot(predicate))
}

object Entity {
  def getNextId: UUID = UUID.randomUUID()

  def apply(id: UUID, typeName: String, components: Seq[EntityComponent]): Entity =
    new Entity(id, typeName, components)

  val playerBounds = new HexBox(0.2f, 0, 1.75f)
  private val sheepBounds = new HexBox(0.4f, 0, 0.75f)
  private val boatBounds = new HexBox(0.8f, 0, 0.1f)

  def atStartPos(
      id: UUID,
      pos: CylCoords,
      entityType: String,
      rotation: Vector3d = new Vector3d
  )(using CylinderSize): Result[Entity, String] = {
    val tag = Nbt
      .makeMap("type" -> Nbt.StringTag(entityType), "id" -> Nbt.StringTag(id.toString))
      .withOptionalField("ai", Option.when(entityType == "sheep")(SimpleWalkAI.create.toNBT))

    Nbt.decode[Entity](tag) match {
      case Some(e) =>
        e.transform.position = pos
        e.transform.rotation.set(rotation)
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
        .withOptionalField(
          "mounts",
          Option.when(e.mountedEntities.nonEmpty) {
            Nbt.ListTag(e.mountedEntities.map(Nbt.encode))
          }
        )
    }
  }

  given (using CylinderSize): NbtDecoder[Entity] with {
    override def decode(tag: Nbt.MapTag): Option[Entity] = {
      val id = tag.getString("id").map(UUID.fromString).getOrElse(UUID.randomUUID())
      val entType = tag.getString("type", "")

      // An entity of unknown type is not a valid entity
      val bounds = entType match {
        case "player" => playerBounds
        case "sheep"  => sheepBounds
        case "boat"   => boatBounds
        case _        => return None
      }

      val components: Seq[EntityComponent] = Seq(
        Some(BoundsComponent(bounds)),
        Nbt.decode[TransformComponent](tag),
        Nbt.decode[MotionComponent](tag),
        Nbt.decode[AiComponent](tag),
        entType match {
          case "player" => Nbt.decode[HeadDirectionComponent](tag)
          case _        => None
        },
        tag
          .getList("mounts")
          .getOrElse(Seq.empty)
          .flatMap(_.asMap)
          .flatMap(Nbt.decode[MountComponent])
      ).flatten

      Some(Entity(id, entType, components))
    }
  }
}

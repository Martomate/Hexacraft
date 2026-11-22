package hexacraft.world.entity

import hexacraft.nbt.{Nbt, NbtDecoder, NbtEncoder}
import hexacraft.util.Channel
import hexacraft.world.{BlocksInWorld, CylinderSize, HexBox}
import hexacraft.world.block.Block
import hexacraft.world.coord.{BlockCoords, BlockRelWorld, CoordUtils, CylCoords}

import org.joml.{Vector3d, Vector3dc}

import java.util.UUID

trait EntityAI {
  def tick(
      world: BlocksInWorld,
      id: UUID,
      transform: TransformComponent,
      velocity: MotionComponent,
      mounting: Option[Entity],
      entityBoundingBox: HexBox,
      events: Channel.Sender[EntityAI.Event]
  ): Unit
  def acceleration: Vector3dc
  def toNBT: Nbt.MapTag
}

class SimpleWalkAI(using CylinderSize) extends EntityAI {
  private val movingForce = new Vector3d
  private[entity] var target: CylCoords = CylCoords(0, 0, 0)

  private[entity] var timeout: Int = 0

  private val timeLimit: Int = 5 * 60
  private val speed = 0.2

  def tick(
      world: BlocksInWorld,
      id: UUID,
      transform: TransformComponent,
      velocity: MotionComponent,
      mounting: Option[Entity],
      entityBoundingBox: HexBox,
      events: Channel.Sender[EntityAI.Event]
  ): Unit = {
    val distSq = transform.position.distanceXZSq(target)

    val reach = if mounting.isDefined then 15 else 5

    movingForce.set(0)
    if timeout > 5 && velocity.velocity.lengthSquared() < speed then {
      timeout = 5
    }

    if distSq < speed * speed || timeout <= 0 then {
      // new goal
      val angle = math.random() * 2 * math.Pi
      val targetX = transform.position.x + reach * math.cos(angle)
      val targetZ = transform.position.z + reach * math.sin(angle)
      target = CylCoords(targetX, 0, targetZ)

      timeout = timeLimit
    } else {
      // move towards goal
      val blockInFrontCoords = coordsInFront(
        transform.position,
        transform.rotation,
        entityBoundingBox.radius + speed * 4
      )

      val blockInFrontBelowCoords = coordsInFront(
        transform.position,
        transform.rotation,
        entityBoundingBox.radius + speed * 4
      ).offset(0, -1, 0)

      val blockInFrontAheadCoords = coordsInFront(
        transform.position,
        transform.rotation,
        entityBoundingBox.radius + speed * 8
      )

      val blockInFront = world.getBlock(blockInFrontCoords).blockType
      val blockInFrontBelow = world.getBlock(blockInFrontBelowCoords).blockType

      if blockInFront != Block.Air && velocity.velocity.y == 0 then {
        movingForce.y = 3.5
      } else if blockInFront == Block.Air && blockInFrontBelow == Block.Water && mounting.isEmpty then {
        events.send(
          EntityAI.Event.SpawnEntity(
            Entity
              .atStartPos(
                UUID.randomUUID(),
                BlockCoords(blockInFrontAheadCoords).toCylCoords,
                "boat",
                transform.rotation
              )
              .unwrap()
              .withComponent(MountComponent(id))
          )
        )
      }
      val angle = transform.position.angleXZ(target)

      movingForce.x = speed * math.cos(angle)
      movingForce.z = -speed * math.sin(angle)
      transform.rotation.y = angle
    }

    timeout -= 1
  }

  private def coordsInFront(position: CylCoords, rotation: Vector3d, dist: Double): BlockRelWorld = {
    val coords = position.offset(dist * math.cos(rotation.y), 0, dist * -math.sin(rotation.y))
    CoordUtils.getEnclosingBlock(coords.toBlockCoords)._1
  }

  override def acceleration: Vector3dc = movingForce

  override def toNBT: Nbt.MapTag = Nbt.encode(this)
}

object EntityAI {
  given (using CylinderSize): NbtDecoder[EntityAI] with {
    override def decode(tag: Nbt.MapTag): Option[EntityAI] = {
      tag.getString("type", "") match {
        case SimpleWalkAI.typeName => Nbt.decode[SimpleWalkAI](tag)
        case _                     => None
      }
    }
  }

  enum Event {
    case SpawnEntity(e: Entity)
  }
}

object SimpleWalkAI {
  val typeName = "simple"

  def create(using CylinderSize): SimpleWalkAI = new SimpleWalkAI

  given NbtEncoder[SimpleWalkAI] with {
    override def encode(ai: SimpleWalkAI): Nbt.MapTag = {
      Nbt.makeMap(
        "type" -> Nbt.StringTag(typeName),
        "targetX" -> Nbt.DoubleTag(ai.target.x),
        "targetZ" -> Nbt.DoubleTag(ai.target.z),
        "timeout" -> Nbt.ShortTag(ai.timeout.toShort)
      )
    }
  }

  given (using CylinderSize): NbtDecoder[SimpleWalkAI] with {
    override def decode(tag: Nbt.MapTag): Option[SimpleWalkAI] = {
      val targetX = tag.getDouble("targetX", 0)
      val targetZ = tag.getDouble("targetZ", 0)
      val timeout = tag.getShort("timeout", 0)

      val ai = new SimpleWalkAI
      ai.target = CylCoords(targetX, 0, targetZ)
      ai.timeout = timeout
      Some(ai)
    }
  }
}

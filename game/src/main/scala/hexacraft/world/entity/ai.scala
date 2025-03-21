package hexacraft.world.entity

import hexacraft.nbt.Nbt
import hexacraft.world.{BlocksInWorld, CylinderSize, HexBox}
import hexacraft.world.block.Block
import hexacraft.world.coord.{BlockRelWorld, CoordUtils, CylCoords}

import org.joml.{Vector3d, Vector3dc}

trait EntityAI {
  def tick(
      world: BlocksInWorld,
      transform: TransformComponent,
      velocity: MotionComponent,
      entityBoundingBox: HexBox
  ): Unit
  def acceleration: Vector3dc
  def toNBT: Nbt.MapTag
}

class SimpleAIInput(using CylinderSize) {
  def blockInFront(world: BlocksInWorld, position: CylCoords, rotation: Vector3d, dist: Double): Block = {
    world.getBlock(blockInFrontCoords(position, rotation, dist)).blockType
  }

  private def blockInFrontCoords(position: CylCoords, rotation: Vector3d, dist: Double): BlockRelWorld = {
    val coords = position.offset(dist * math.cos(rotation.y), 0, dist * -math.sin(rotation.y))
    CoordUtils.getEnclosingBlock(coords.toBlockCoords)._1
  }
}

class SimpleWalkAI(using CylinderSize) extends EntityAI {
  private val movingForce = new Vector3d
  private var target: CylCoords = CylCoords(0, 0, 0)

  private var timeout: Int = 0

  private val timeLimit: Int = 5 * 60
  private val reach: Double = 5
  private val speed = 0.2

  private val input: SimpleAIInput = new SimpleAIInput

  def tick(
      world: BlocksInWorld,
      transform: TransformComponent,
      velocity: MotionComponent,
      entityBoundingBox: HexBox
  ): Unit = {
    val distSq = transform.position.distanceXZSq(target)

    movingForce.set(0)

    if distSq < speed * speed || timeout == 0 then {
      // new goal
      val angle = math.random() * 2 * math.Pi
      val targetX = transform.position.x + reach * math.cos(angle)
      val targetZ = transform.position.z + reach * -math.sin(angle)
      target = CylCoords(targetX, 0, targetZ)

      timeout = timeLimit
    } else {
      // move towards goal
      val blockInFront =
        input.blockInFront(
          world,
          transform.position,
          transform.rotation,
          entityBoundingBox.radius + speed * 4
        )

      if blockInFront != Block.Air && velocity.velocity.y == 0 then {
        movingForce.y = 3.5
      }
      val angle = transform.position.angleXZ(target)

      movingForce.x = speed * math.cos(angle)
      movingForce.z = speed * math.sin(angle)
      transform.rotation.y = -angle
    }

    timeout -= 1

    /*    val speed = 40
    val velLen = math.hypot(player.velocity.x, player.velocity.z)
    if (velLen > speed) {
      velocity.x *= speed / velLen
      velocity.z *= speed / velLen
    }*/
  }

  override def acceleration: Vector3dc = movingForce

  override def toNBT: Nbt.MapTag = Nbt.makeMap(
    "type" -> Nbt.StringTag("simple"),
    "targetX" -> Nbt.DoubleTag(target.x),
    "targetZ" -> Nbt.DoubleTag(target.z),
    "timeout" -> Nbt.ShortTag(timeout.toShort)
  )
}

object SimpleWalkAI {
  def create(using CylinderSize): SimpleWalkAI = new SimpleWalkAI

  def fromNBT(tag: Nbt.MapTag)(using CylinderSize): SimpleWalkAI = {
    val targetX = tag.getDouble("targetX", 0)
    val targetZ = tag.getDouble("targetZ", 0)
    val target = tag.getShort("timeout", 0)

    val ai = new SimpleWalkAI
    ai.target = CylCoords(targetX, 0, targetZ)
    ai.timeout = target
    ai
  }
}

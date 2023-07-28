package hexacraft.world.entity.ai

import com.flowpowered.nbt.*
import hexacraft.nbt.{Nbt, NBTUtil}
import hexacraft.world.{BlocksInWorld, CylinderSize}
import hexacraft.world.block.{Blocks, HexBox}
import hexacraft.world.coord.fp.CylCoords
import hexacraft.world.entity.{Entity, EntityBaseData}
import org.joml.{Vector3d, Vector3dc}

class SimpleWalkAI(using CylinderSize)(using Blocks: Blocks) extends EntityAI {
  private val movingForce = new Vector3d
  private var target: CylCoords = CylCoords(0, 0, 0)

  private var timeout: Int = 0

  private val timeLimit: Int = 5 * 60
  private val reach: Double = 5
  private val speed = 0.2

  private val input: SimpleAIInput = new SimpleAIInput

  def tick(world: BlocksInWorld, entityBaseData: EntityBaseData, entityBoundingBox: HexBox): Unit = {
    val distSq = entityBaseData.position.distanceXZSq(target)

    movingForce.set(0)

    if (distSq < speed * speed || timeout == 0) {
      // new goal
      val angle = math.random() * 2 * math.Pi
      val targetX = entityBaseData.position.x + reach * math.cos(angle)
      val targetZ = entityBaseData.position.z + reach * -math.sin(angle)
      target = CylCoords(targetX, 0, targetZ)

      timeout = timeLimit
    } else {
      // move towards goal
      val blockInFront =
        input.blockInFront(
          world,
          entityBaseData.position,
          entityBaseData.rotation,
          entityBoundingBox.radius + speed * 4
        )

      if (blockInFront != Blocks.Air && entityBaseData.velocity.y == 0) {
        movingForce.y = 3.5
      }
      val angle = entityBaseData.position.angleXZ(target)

      movingForce.x = speed * math.cos(angle)
      movingForce.z = speed * math.sin(angle)
      entityBaseData.rotation.y = -angle
    }

    timeout -= 1

    /*    val speed = 40
    val velLen = math.hypot(player.velocity.x, player.velocity.z)
    if (velLen > speed) {
      velocity.x *= speed / velLen
      velocity.z *= speed / velLen
    }*/
  }

  override def acceleration(): Vector3dc = movingForce

  override def toNBT: Seq[Tag[_]] = Seq(
    new StringTag("type", "simple"),
    new DoubleTag("targetX", target.x),
    new DoubleTag("targetZ", target.z),
    new ShortTag("timeout", timeout.toShort)
  )
}

object SimpleWalkAI:
  def create(using CylinderSize, Blocks): SimpleWalkAI = new SimpleWalkAI

  def fromNBT(tag: Nbt.MapTag)(using CylinderSize, Blocks): SimpleWalkAI = {
    val targetX = tag.getDouble("targetX", 0)
    val targetZ = tag.getDouble("targetZ", 0)
    val target = tag.getShort("timeout", 0)

    val ai = new SimpleWalkAI
    ai.target = CylCoords(targetX, 0, targetZ)
    ai.timeout = target
    ai
  }

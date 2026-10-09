package hexacraft.server.world

import hexacraft.physics.DragCoefficient
import hexacraft.world.{CollisionDetector, CylinderSize, FakeBlocksInWorld, HexBox}
import hexacraft.world.block.{Block, BlockState}
import hexacraft.world.coord.{BlockRelWorld, CylCoords}
import hexacraft.world.entity.{EntityModel, EntityPart, HexPrism, MotionComponent, TransformComponent}

import munit.FunSuite
import org.joml.Vector3f

class EntityPhysicsSystemTest extends FunSuite {
  given CylinderSize = CylinderSize(8)

  private val bounds = HexBox(0.1f, 0, 0.5f)
  private val volume = VolumeSamples.fromModel(
    EntityModel(IndexedSeq(EntityPart("body", HexPrism(8, 32), Vector3f(), Vector3f())))
  )

  /** A world that is filled with water around the origin, from y = -2 to y = 2 */
  private def deepWater(): FakeBlocksInWorld = {
    val blocks = for {
      y <- -4 to 3
      x <- -4 to 4
      z <- -4 to 4
    } yield BlockRelWorld(x, y, z) -> BlockState(Block.Water, 0)
    FakeBlocksInWorld.withBlocks(blocks.toMap)
  }

  /** Lets an entity with the given density start at rest under water, and returns its vertical velocity after a tick */
  private def verticalVelocityAfterOneTick(
      density: Double,
      dragCoefficient: DragCoefficient = DragCoefficient.human
  ): Double = {
    val world = deepWater()
    val physics = EntityPhysicsSystem(world, CollisionDetector(world))

    val transform = TransformComponent(CylCoords(0, 0, 0))
    val motion = MotionComponent()
    val mass = volume.totalVolume * density

    physics.update(transform, motion, bounds, volume, mass, dragCoefficient)
    motion.velocity.y
  }

  test("an entity as dense as water stays still under water") {
    assertEqualsDouble(verticalVelocityAfterOneTick(1000), 0, 1e-3)
  }

  test("an entity less dense than water rises in water") {
    assert(verticalVelocityAfterOneTick(600) > 0.01)
  }

  test("an entity more dense than water sinks in water") {
    assert(verticalVelocityAfterOneTick(2000) < -0.01)
  }

  test("buoyancy pushes up with the weight of the displaced water") {
    // a = (V * rho_water - m) * g / m, and the velocity changes by a / 60 in one tick
    // a = (V * rho_water - V * rho_entity) * g / V / rho_entity
    // a = (rho_water - rho_entity) * g / rho_entity
    val density = 600.0
    val expected = (1000 - density) * 9.82 / density / 60

    // there is no drag, since drag would slow down the entity as soon as it starts moving
    val noDrag = DragCoefficient.fromDouble(0)
    assertEqualsDouble(verticalVelocityAfterOneTick(density, noDrag), expected, 1e-9)
  }
}

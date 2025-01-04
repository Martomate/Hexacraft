package hexacraft.world

import hexacraft.math.MathUtils
import hexacraft.util.Loop
import hexacraft.world.coord.{BlockCoords, BlockRelWorld, CoordUtils, CylCoords, Offset, SkewCylCoords}

import org.joml.Vector3d

class MovingBox(val bounds: HexBox, val pos: CylCoords, val velocity: CylCoords.Offset)

class CollisionDetector(world: BlocksInWorld)(using cylSize: CylinderSize) {
  private val reflectionDirs = Array(
    Offset(0, -1, 0),
    Offset(0, 1, 0),
    Offset(-1, 0, 0),
    Offset(1, 0, 0),
    Offset(0, 0, -1),
    Offset(0, 0, 1),
    Offset(1, 0, -1),
    Offset(-1, 0, 1)
  )
  private val reflDirsCyl = for Offset(x, y, z) <- reflectionDirs yield {
    SkewCylCoords.Offset(x, y, z).toCylCoordsOffset
  }

  private val chunkCache: ChunkCache = new ChunkCache(world)

  def collides(
      objectBounds: HexBox,
      objectCoords: CylCoords,
      targetBounds: HexBox,
      targetCoords: CylCoords
  ): Boolean = {
    val box = new MovingBox(objectBounds, objectCoords, CylCoords.Offset(0, 0, 0))
    distanceToCollision(box, targetBounds, targetCoords.toSkewCylCoords)._1 == 0
  }

  /** pos and velocity should be CylCoords in vector form. Velocity is per tick. */
  def positionAndVelocityAfterCollision(
      box: HexBox,
      pos: Vector3d,
      velocity: Vector3d
  ): (Vector3d, Vector3d) = {
    chunkCache.clearCache()

    val vel = new Vector3d(velocity)
    var result = (pos, vel)
    val parts = (velocity.length * 10).toInt + 1
    result._2.div(parts)

    Loop.rangeUntil(0, parts) { _ =>
      val currentPos = CylCoords(result._1)
      val currentVelocity = CylCoords.Offset(vel)
      result = _collides(new MovingBox(box, currentPos, currentVelocity), 100)
    }

    result._2.mul(parts)
    result
  }

  private def _collides(box: MovingBox, ttl: Int): (Vector3d, Vector3d) = {
    if ttl < 0 then { // TODO: this is a temporary fix for the StackOverflow problem
      return (box.pos.toVector3d, new Vector3d)
    }

    if box.velocity.x == 0 && box.velocity.y == 0 && box.velocity.z == 0 then { // velocity is 0
      return (box.pos.toVector3d, box.velocity.toVector3d)
    }

    val futureCoords = box.pos.offset(box.velocity).toBlockCoords
    val (bc, _) = CoordUtils.getEnclosingBlock(futureCoords)

    minDistAndReflectionDir(box, bc) match {
      case Some((minDist, reflectionDir)) =>
        resultAfterCollision(box, minDist, reflectionDir, ttl)
      case None =>
        (box.pos.offset(box.velocity).toVector3d, box.velocity.toVector3d) // no collision found
    }
  }

  /** This will check all blocks that could intersect the bounds of the object. If any block
    * intersects the bounds of the object it will return (dist: 0, side: -1) If no blocks are in the
    * way it will return None Otherwise it will return (dist: 'distance to the closest block', side:
    * 'side of collision for that block')
    */
  private def minDistAndReflectionDir(box: MovingBox, bc: BlockRelWorld): Option[(Double, Int)] = {
    val yLo = math.floor((box.pos.y + box.velocity.y + box.bounds.bottom) * 2).toInt
    val yHi = math.floor((box.pos.y + box.velocity.y + box.bounds.top) * 2).toInt

    // min by dist, with dir as extra data
    var minDist: Double = Double.MaxValue
    var minDir: Int = 0

    Loop.rangeTo(yLo, yHi) { y =>
      Loop.rangeUntil(0, 9) { i =>
        val dx = (i % 3) - 1
        val dz = (i / 3) - 1

        if dx * dz != 1 then { // remove corners
          // keep track of the closest block we are about to collide with
          distanceToBlock(box, BlockRelWorld(bc.x + dx, y, bc.z + dz)) match {
            case Some((dist, dir)) =>
              if dist < minDist then {
                minDist = dist
                minDir = dir
              }
            case None =>
          }
        }
      }
    }

    // only return the distance if there was in fact a collision
    if minDist != Double.MaxValue then {
      Some((minDist, minDir))
    } else {
      None
    }
  }

  /** Returns the distance to the target block along `vec` and the side of the collision.
    *
    * If the chunk of the target block is not loaded it will return (dist: 0, side: -1), which is
    * the same as if the object was intersecting the block.
    *
    * If the target block is air it will return None
    */
  private def distanceToBlock(box: MovingBox, targetBlock: BlockRelWorld): Option[(Double, Int)] = {
    val chunk = chunkCache.getChunk(targetBlock.getChunkRelWorld)

    // Chunk isn't loaded, you're stuck (so that you don't fall into the void or something)
    if chunk == null then {
      return Some((0, -1))
    }

    val blockState = chunk.getBlock(targetBlock.getBlockRelChunk)
    if !blockState.blockType.isSolid then {
      return None
    }

    val targetBounds = blockState.blockType.bounds(blockState.metadata)
    val targetCoords = BlockCoords(targetBlock).toSkewCylCoords

    Some(distanceToCollision(box, targetBounds, targetCoords))
  }

  private def resultAfterCollision(
      box: MovingBox,
      minDist: Double,
      reflectionDir: Int,
      ttl: Int
  ): (Vector3d, Vector3d) = {
    if minDist >= 1d then { // no collision found
      return (box.pos.offset(box.velocity).toVector3d, box.velocity.toVector3d)
    }

    if reflectionDir == -1 then { // inside a block
      return (box.pos.toVector3d, new Vector3d)
    }

    val normal = reflDirsCyl(reflectionDir).toVector3d.normalize()
    val newPos = box.pos.offset(box.velocity.x * minDist, box.velocity.y * minDist, box.velocity.z * minDist)
    val vel = box.velocity.toVector3d.mul(1 - minDist)
    val dot = vel.dot(normal)
    vel.sub(normal.mul(dot))
    val result = _collides(new MovingBox(box.bounds, newPos, CylCoords.Offset(vel)), ttl - 1)
    result._2.mul(1 / (1 - minDist))
    result
  }

  /** Returns the distance to the other object along the vector `vec`. Also returns the side of the
    * other object that will be collided with.
    *
    * If the objects are already intersecting, it will return (dist: 1, side: -1) The maximum
    * distance returned is 1 (meaning the full length of `vec`). If no collision is found within
    * that distance it will return (dist: 1, side: -1) Otherwise it will return (dist: 'distance in
    * units of `vec.length`, side: 'side of collision')
    */
  private def distanceToCollision(
      box1: MovingBox,
      box2: HexBox,
      _pos2: SkewCylCoords
  ): (Double, Int) = {
    val vel1 = box1.velocity.toSkewCylCoordsOffset
    val pos1 = box1.pos.toSkewCylCoords.offset(vel1) // pos after moving
    // The following line ensures that the code works when z is close to 0
    val pos2 = SkewCylCoords.Offset(
      _pos2.x,
      _pos2.y,
      MathUtils.absmin(_pos2.z - pos1.z, cylSize.circumference) + pos1.z
    )

    val x1 = pos1.x + 0.5 * pos1.z
    val y1 = pos1.y
    val z1 = pos1.z + 0.5 * pos1.x
    val x2 = pos2.x + 0.5 * pos2.z
    val y2 = pos2.y
    val z2 = pos2.z + 0.5 * pos2.x

    val r1 = box1.bounds.smallRadius
    val r2 = box2.smallRadius
    val b1 = box1.bounds.bottom
    val b2 = box2.bottom
    val t1 = box1.bounds.top
    val t2 = box2.top

    val dx = x2 - x1
    val dy = y2 - y1
    val dz = z2 - z1
    val d = r2 + r1

    val vx = vel1.x + 0.5 * vel1.z
    val vy = vel1.y
    val vz = vel1.z + 0.5 * vel1.x

    // index corresponds to `reflectionDirs`
    val distances = Array(
      t2 - b1 + dy, // (  y2    + t2) - (  y1    + b1),
      t1 - b2 - dy, // (  y1    + t1) - (  y2    + b2),
      d + dx, //       (     x2 + r2) - (     x1 - r1),
      d - dx, //       (     x1 + r1) - (     x2 - r2),
      d + dz, //       (z2      + r2) - (z1      - r1),
      d - dz, //       (z1      + r1) - (z2      - r2),
      d + dz - dx, //  (z2 - x2 + r2) - (z1 - x1 - r1),
      d - dz + dx //   (z1 - x1 + r1) - (z2 - x2 - r2)
    )

    Loop.array(distances) { d =>
      if d < 0 then { // the box is not colliding after moving
        return (1, -1)
      }
    }

    var minDist = 1.0
    var minDistDir = -1

    Loop.rangeUntil(0, 8) { i =>
      val t = reflectionDirs(i)
      val velDist = t.dx * vx + t.dy * vy + t.dz * vz // the length of v along the normals
      val distAfter = ((velDist - distances(i)) * 1e9).toLong / 1e9

      if velDist > 0 && distAfter >= 0 then {
        val a = distAfter / velDist
        if a < minDist then {
          minDist = a
          minDistDir = i
        }
      }
    }

    if minDistDir != -1 then {
      (math.min(minDist, 1), minDistDir)
    } else {
      (0, -1) // the box was colliding even before moving
    }
  }
}

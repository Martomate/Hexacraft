package hexacraft.game

import hexacraft.math.MathUtils.oppositeSide
import hexacraft.world.{Camera, CylinderSize, HexBox}
import hexacraft.world.block.{Block, BlockState}
import hexacraft.world.coord.*

import org.joml.{Vector2fc, Vector3d, Vector4f}

import scala.annotation.tailrec

class RayTracer(camera: Camera, maxDistance: Double)(using CylinderSize) {
  def trace(ray: Ray, blockAtCoords: BlockRelWorld => Option[BlockState]): Option[(BlockRelWorld, Option[Int])] = {
    if blockTouched(blockAtCoords, ray, camera.blockCoords) then {
      Some((camera.blockCoords, None))
    } else {
      traceIt(camera.blockCoords, ray, blockAtCoords, 1000)
    }
  }

  @tailrec
  private def traceIt(
      current: BlockRelWorld,
      ray: Ray,
      blockAtCoords: BlockRelWorld => Option[BlockState],
      ttl: Int
  ): Option[(BlockRelWorld, Option[Int])] = {
    if ttl < 0 then { // TODO: this is a temporary fix for ray-loops
      return None
    }

    val points = PointHexagon.fromHexBox(BlockState.boundingBox, current, camera)

    val index = sideIndex(ray, points)
    val side = actualSide(ray, points, index)
    val normal = sideNormal(points, index, side)

    if ray.v.dot(normal) > 0 then { // TODO: this is a temporary fix for ray-loops
      System.err.println("At least one bug has not been figured out yet! (Rayloops in RayTracer.trace.traceIt)")
      return None
    }

    val pointOnSide = if side == 0 then points.up(index) else points.down(index)
    val distance = Math.abs(pointOnSide.dot(normal) / ray.v.dot(normal)) // abs may be needed (a/-0)
    if distance > maxDistance * CylinderSize.y60 then {
      return None
    }

    val hitBlockCoords = current.offset(NeighborOffsets(side))
    if blockTouched(blockAtCoords, ray, hitBlockCoords) then {
      return Some((hitBlockCoords, Some(oppositeSide(side))))
    }

    traceIt(hitBlockCoords, ray, blockAtCoords, ttl - 1)
  }

  private def sideNormal(points: PointHexagon, index: Int, side: Int) = {
    val PA = new Vector3d
    val PB = new Vector3d

    if side == 0 then {
      points.up((index + 1) % 6).sub(points.up(index), PA)
      points.up((index + 5) % 6).sub(points.up(index), PB)
    } else if side == 1 then {
      points.down((index + 5) % 6).sub(points.down(index), PA)
      points.down((index + 1) % 6).sub(points.down(index), PB)
    } else {
      points.down((index + 1) % 6).sub(points.down(index), PA)
      points.up(index).sub(points.down(index), PB)
    }

    PA.cross(PB, new Vector3d())
  }

  private def sideIndex(ray: Ray, points: PointHexagon) = {
    if ray.toTheRight(points.down(0), points.up(0)) then {
      (5 to 1 by -1).find(index => !ray.toTheRight(points.down(index), points.up(index))).getOrElse(0)
    } else {
      (1 to 5).find(index => ray.toTheRight(points.down(index), points.up(index))).getOrElse(6) - 1
    }
  }

  private def actualSide(ray: Ray, points: PointHexagon, index: Int) = {
    if ray.toTheRight(points.up(index), points.up((index + 1) % 6)) then {
      0
    } else if !ray.toTheRight(points.down(index), points.down((index + 1) % 6)) then {
      1
    } else {
      index + 2
    }
  }

  private def blockTouched(
      blockAtCoords: BlockRelWorld => Option[BlockState],
      ray: Ray,
      hitBlockCoords: BlockRelWorld
  ): Boolean = {
    blockAtCoords(hitBlockCoords) match {
      case Some(block) if block.blockType != Block.Air =>
        (0 until 8).exists(side => {
          val boundingBox = block.blockType.bounds(block.metadata)
          val points = PointHexagon.fromHexBox(boundingBox, hitBlockCoords, camera)
          ray.intersectsPolygon(points, side)
        })
      case _ =>
        false
    }
  }
}

class Ray(val v: Vector3d) {

  /** @return
    * true if the ray goes to the right of the line from `up` to `down` in a reference frame where
    * `up` is directly above `down` (i.e. possibly rotated)
    */
  def toTheRight(down: Vector3d, up: Vector3d): Boolean = {
    down.dot(up.cross(v, new Vector3d)) <= 0
  }

  def intersectsPolygon(points: PointHexagon, side: Int): Boolean = {
    val rightSeq = side match {
      case 0 => for index <- 0 until 6 yield this.toTheRight(points.up(index), points.up((index + 1) % 6))
      case 1 => for index <- 0 until 6 yield this.toTheRight(points.down(index), points.down((index + 1) % 6))
      case _ =>
        val order = Seq(0, 1, 3, 2)

        for index <- 0 until 4 yield {
          val aIdx = (order(index) % 2 + side - 2) % 6
          val PA = if order(index) / 2 == 0 then points.up(aIdx) else points.down(aIdx)

          val bIdx = (order((index + 1) % 4) % 2 + side - 2) % 6
          val PB = if order((index + 1) % 4) / 2 == 0 then points.up(bIdx) else points.down(bIdx)

          this.toTheRight(PA, PB)
        }
    }

    allElementsSame(rightSeq)
  }

  private def allElementsSame(seq: IndexedSeq[Boolean]) = {
    !seq.exists(_ != seq(0))
  }
}

object Ray {

  /** @param coords normalized screen coordinates */
  def fromScreen(camera: Camera, coords: Vector2fc): Option[Ray] = {
    if coords.x < -1 || coords.x > 1 || coords.y < -1 || coords.y > 1 then {
      return None
    }

    val coords4 = new Vector4f(coords.x, coords.y, -1, 1)
    coords4.mul(camera.proj.invMatrix)
    coords4.set(coords4.x, coords4.y, -1, 0)
    coords4.mul(camera.view.invMatrix)

    val ray = new Vector3d(coords4.x, coords4.y, coords4.z)
    ray.normalize()

    Some(Ray(ray))
  }
}

object PointHexagon {
  def fromHexBox(hexBox: HexBox, location: BlockRelWorld, camera: Camera)(using CylinderSize): PointHexagon = {
    val points = hexBox.vertices.map(v =>
      BlockCoords(location).toCylCoords
        .offset(v)
        .toNormalCoords(CylCoords(camera.view.position))
        .toVector3d
    )
    new PointHexagon(points)
  }
}

class PointHexagon(points: Seq[Vector3d]) {
  def up(idx: Int): Vector3d = points(idx)
  def down(idx: Int): Vector3d = points(idx + 6)
}

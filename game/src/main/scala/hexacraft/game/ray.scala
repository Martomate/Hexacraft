package hexacraft.game

import hexacraft.game.PointHexagon.{Region, Slice}
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

    val (slice, region) = points.intersectionSide(ray)
    val normal = points.normal(slice, region)

    if ray.v.dot(normal) > 0 then { // TODO: this is a temporary fix for ray-loops
      System.err.println("At least one bug has not been figured out yet! (Rayloops in RayTracer.trace.traceIt)")
      return None
    }

    val pointOnSide = if region == Region.Ceiling then points.up(slice) else points.down(slice)
    val distance = Math.abs(pointOnSide.dot(normal) / ray.v.dot(normal)) // abs may be needed (a/-0)
    if distance > maxDistance * CylinderSize.y60 then {
      return None
    }

    val side = region match {
      case Region.Ceiling => 0
      case Region.Floor   => 1
      case Region.Wall    => slice + 2
    }

    val hitBlockCoords = current.offset(NeighborOffsets(side))
    if blockTouched(blockAtCoords, ray, hitBlockCoords) then {
      return Some((hitBlockCoords, Some(oppositeSide(side))))
    }

    traceIt(hitBlockCoords, ray, blockAtCoords, ttl - 1)
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
          ray.intersectsPolygon(points, BlockFace.fromInt(side))
        })
      case _ =>
        false
    }
  }
}

enum BlockFace {
  case Top
  case Bottom

  /** @param index 0 to 5, ccw */
  case Side(index: Int)
}

object BlockFace {
  def fromInt(i: Int): BlockFace = {
    i match {
      case 0 => BlockFace.Top
      case 1 => BlockFace.Bottom
      case _ => BlockFace.Side(i - 2)
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

  def intersectsPolygon(points: PointHexagon, side: BlockFace): Boolean = {
    val rightSeq = side match {
      case BlockFace.Top =>
        for index <- 0 until 6 yield {
          val a = points.up(index)
          val b = points.up((index + 1) % 6)
          this.toTheRight(a, b)
        }
      case BlockFace.Bottom =>
        for index <- 0 until 6 yield {
          val a = points.down(index)
          val b = points.down((index + 1) % 6)
          this.toTheRight(a, b)
        }
      case BlockFace.Side(s) =>
        val order = Seq(0, 1, 3, 2)

        for index <- 0 until 4 yield {
          val aIdx = (order(index) % 2 + s) % 6
          val bIdx = (order((index + 1) % 4) % 2 + s) % 6

          val aUp = order(index) / 2 == 0
          val bUp = order((index + 1) % 4) / 2 == 0

          val PA = if aUp then points.up(aIdx) else points.down(aIdx)
          val PB = if bUp then points.up(bIdx) else points.down(bIdx)

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
    new PointHexagon(points.take(6).toArray, points.drop(6).toArray)
  }

  type Slice = Int

  enum Region {
    case Ceiling
    case Floor
    case Wall
  }
}

class PointHexagon(val up: Array[Vector3d], val down: Array[Vector3d]) {
  extension (index: Int) {
    private inline def inc: Int = (index + 1) % 6
    private inline def dec: Int = (index + 5) % 6
  }

  def intersectionSide(ray: Ray): (Slice, Region) = {
    // check the sides of the sides of a hexagonal pillar
    val index = if ray.toTheRight(this.down(0), this.up(0)) then {
      (5 to 1 by -1).find(index => !ray.toTheRight(this.down(index), this.up(index))).getOrElse(0)
    } else {
      (1 to 5).find(index => ray.toTheRight(this.down(index), this.up(index))).getOrElse(6) - 1
    }

    // check for intersection with the ceiling or the floor
    val region = if ray.toTheRight(this.up(index), this.up(index.inc)) then {
      Region.Ceiling
    } else if !ray.toTheRight(this.down(index), this.down(index.inc)) then {
      Region.Floor
    } else {
      Region.Wall
    }

    (index, region)
  }

  def normal(index: Slice, region: Region): Vector3d = {
    val PA = new Vector3d
    val PB = new Vector3d

    region match {
      case Region.Ceiling =>
        this.up(index.inc).sub(this.up(index), PA)
        this.up(index.dec).sub(this.up(index), PB)
      case Region.Floor =>
        this.down(index.dec).sub(this.down(index), PA)
        this.down(index.inc).sub(this.down(index), PB)
      case Region.Wall =>
        this.down(index.inc).sub(this.down(index), PA)
        this.up(index).sub(this.down(index), PB)
    }

    PA.cross(PB, new Vector3d())
  }
}

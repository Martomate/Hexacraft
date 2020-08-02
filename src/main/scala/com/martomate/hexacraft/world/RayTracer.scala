package com.martomate.hexacraft.world

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.Blocks
import com.martomate.hexacraft.world.block.state.BlockState
import com.martomate.hexacraft.world.camera.Camera
import com.martomate.hexacraft.world.coord.CoordUtils
import com.martomate.hexacraft.world.coord.fp.{BlockCoords, CylCoords}
import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, NeighborOffsets, Offset}
import com.martomate.hexacraft.world.worldlike.IWorld
import org.joml.{Vector2fc, Vector3d, Vector4f}

import scala.collection.immutable


class RayTracer(world: IWorld, camera: Camera, maxDistance: Double) {
  import world.size.impl

  private val ray = new Vector3d()
  private var rayValid = false

  def setRayFromScreen(normalizedScreenCoords: Vector2fc): Unit = {
    val coords = normalizedScreenCoords
    if (coords.x < -1 || coords.x > 1 || coords.y < -1 || coords.y > 1) {
      rayValid = false
    } else {
      rayValid = true
      val coords4 = new Vector4f(coords.x, coords.y, -1, 1)
      coords4.mul(camera.proj.invMatrix)
      coords4.set(coords4.x, coords4.y, -1, 0)
      coords4.mul(camera.view.invMatrix)
      ray.set(coords4.x, coords4.y, coords4.z)
      ray.normalize()
    }
  }

  private def toTheRight(PA: Vector3d, PB: Vector3d): Boolean = PA.dot(PB.cross(ray, new Vector3d)) <= 0

  private def sideIndex(points: Seq[Vector3d]) = {
    if (toTheRight(points(0 + 6), points(0))) {
      (5 to 1 by -1).find(index => !toTheRight(points(index + 6), points(index))).getOrElse(0)
    } else {
      (1 to 5).find(index => toTheRight(points(index + 6), points(index))).getOrElse(6) - 1
    }
  }

  private def actualSide(points: Seq[Vector3d], PA: Vector3d, PB: Vector3d, index: Int) = {
    PA set points((index + 1) % 6)
    PB set points(index)

    if (toTheRight(PA, PB)) { // under ceiling
      PA set points(index + 6)
      PB set points((index + 1) % 6 + 6)

      if (toTheRight(PA, PB)) index + 2 // over floor
      else 1
    } else 0
  }

  private def setAB_dependingOnSide(points: Seq[Vector3d], PA: Vector3d, PB: Vector3d, index: Int, side: Int) = {
    if (side == 0) {
      points((index + 1) % 6).sub(points(index), PA)
      points((index + 5) % 6).sub(points(index), PB)
    } else if (side == 1) {
      points((index + 5) % 6 + 6).sub(points(index + 6), PA)
      points((index + 1) % 6 + 6).sub(points(index + 6), PB)
    } else {
      points((index + 1) % 6 + 6).sub(points(index + 6), PA)
      points(index).sub(points(index + 6), PB)
    }
  }

  private def coordsOfHitBlock(current: BlockRelWorld, offsets: Offset) = {
    BlockRelWorld(current.x + offsets.dx, current.y + offsets.dy, current.z + offsets.dz)
  }

  private def oppositeSide(side: Int) = {
    if (side < 2) 1 - side else (side + 1) % 6 + 2 // (side - 2 + 3) % 6 + 2
  }

  private def getSeqForTopOrBottom(points: Seq[Vector3d], PA: Vector3d, PB: Vector3d, side: Int) = {
    for (index <- 0 until 6) yield {
      PA set points(index + 6 * side)
      PB set points((index + 1) % 6 + 6 * side)

      toTheRight(PA, PB)
    }
  }

  private def getSeqForSide(points: Seq[Vector3d], PA: Vector3d, PB: Vector3d, side: Int) = {
    val order = Seq(0, 1, 3, 2)
    for (index <- 0 until 4) yield {
      PA set points((order(index) % 2 + side - 2) % 6 + 6 * (order(index) / 2))
      PB set points((order((index + 1) % 4) % 2 + side - 2) % 6 + 6 * (order((index + 1) % 4) / 2))

      toTheRight(PA, PB)
    }
  }

  def fromBlockCoords(blockPos: BlockRelWorld, position: CylCoords, _result: Vector3d): Vector3d =
    CoordUtils.fromBlockCoords(CylCoords(camera.view.position), BlockCoords(blockPos), position, _result)

  private def blockTouched(hitBlockCoords: BlockRelWorld): Boolean = world.getBlock(hitBlockCoords) match {
    case block if block.blockType != Blocks.Air =>
      val points = for (v <- block.blockType.bounds(block.metadata).vertices) yield {
        fromBlockCoords(hitBlockCoords, v, new Vector3d)
      }
      val PA = new Vector3d
      val PB = new Vector3d

      (0 until 8).exists(side => {
        val seq = if (side < 2) {
          getSeqForTopOrBottom(points, PA, PB, side)
        } else {
          getSeqForSide(points, PA, PB, side)
        }
        allElementsSame(seq)
      })
    case _ => false
  }

  private def allElementsSame(seq: immutable.IndexedSeq[Boolean]) = {
    !seq.exists(_ != seq(0))
  }

  private def traceIt(current: BlockRelWorld, blockFoundFn: BlockRelWorld => Boolean): Option[(BlockRelWorld, Option[Int])] = {
    val points = BlockState.vertices.map(v => fromBlockCoords(current, v, new Vector3d))

    val PA = new Vector3d
    val PB = new Vector3d

    val index = sideIndex(points)
    val side = actualSide(points, PA, PB, index)

    setAB_dependingOnSide(points, PA, PB, index, side)
    val normal = PA.cross(PB, new Vector3d())

    if (ray.dot(normal) <= 0) { // TODO: this is a temporary fix for ray-loops
      val pointOnSide = points(if (side == 0) index else index + 6)
      val distance = Math.abs(pointOnSide.dot(normal) / ray.dot(normal)) // abs may be needed (a/-0)
      if (distance <= maxDistance * CylinderSize.y60) {
        val hitBlockCoords = coordsOfHitBlock(current, NeighborOffsets(side))

        if (blockFoundFn(hitBlockCoords) && blockTouched(hitBlockCoords)) {
          Some((hitBlockCoords, Some(oppositeSide(side))))
        } else traceIt(hitBlockCoords, blockFoundFn)
      } else None
    } else {
      System.err.println("At least one bug has not been figured out yet! (Rayloops in RayTracer.trace.traceIt)")
      None
    }
  }

  def trace(blockFoundFn: BlockRelWorld => Boolean): Option[(BlockRelWorld, Option[Int])] = {
    if (!rayValid) None
    else if (blockFoundFn(camera.blockCoords) && blockTouched(camera.blockCoords)) Some((camera.blockCoords, None))
    else traceIt(camera.blockCoords, blockFoundFn)
  }
}

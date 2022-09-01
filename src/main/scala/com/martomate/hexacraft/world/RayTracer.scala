package com.martomate.hexacraft.world

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.Blocks
import com.martomate.hexacraft.world.block.state.BlockState
import com.martomate.hexacraft.world.camera.Camera
import com.martomate.hexacraft.world.coord.fp.{BlockCoords, CylCoords}
import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, NeighborOffsets}
import org.joml.{Vector2fc, Vector3d, Vector3dc, Vector4f}

import scala.annotation.tailrec
import com.martomate.hexacraft.world.coord.fp.NormalCoords.apply
import com.martomate.hexacraft.world.coord.fp.NormalCoords

object Ray {
  def fromScreen(camera: Camera, normalizedScreenCoords: Vector2fc): Option[Ray] = {
    val coords = normalizedScreenCoords
    if (coords.x < -1 || coords.x > 1 || coords.y < -1 || coords.y > 1) {
      None
    } else {
      val coords4 = new Vector4f(coords.x, coords.y, -1, 1)
      coords4.mul(camera.proj.invMatrix)
      coords4.set(coords4.x, coords4.y, -1, 0)
      coords4.mul(camera.view.invMatrix)
      val ray = new Vector3d(coords4.x, coords4.y, coords4.z)
      ray.normalize()
      Some(Ray(ray))
    }
  }
}

class Ray(val v: Vector3d):

  /** @return
    *   true if the ray goes to the right of the line from `up` to `down` in a reference frame where
    *   `up` is directly above `down` (i.e. possibly rotated)
    */
  def toTheRight(down: Vector3d, up: Vector3d): Boolean = down.dot(up.cross(v, new Vector3d)) <= 0

  def intersectsPolygon(points: Seq[Vector3d], side: Int): Boolean = {
    if side < 2 then
      val rightSeq =
        for index <- 0 until 6
        yield
          val PA = points(index + 6 * side)
          val PB = points((index + 1) % 6 + 6 * side)

          this.toTheRight(PA, PB)

      rightSeq.allElementsSame
    else
      val order = Seq(0, 1, 3, 2)
      val rightSeq =
        for (index <- 0 until 4) yield
          val PA = points((order(index) % 2 + side - 2) % 6 + 6 * (order(index) / 2))
          val PB = points(
            (order((index + 1) % 4) % 2 + side - 2) % 6 + 6 * (order((index + 1) % 4) / 2)
          )

          this.toTheRight(PA, PB)
      rightSeq.allElementsSame
  }

  extension (seq: IndexedSeq[Boolean]) def allElementsSame = !seq.exists(_ != seq(0))

class RayTracer(world: BlocksInWorld, camera: Camera, maxDistance: Double)(implicit
    cylSize: CylinderSize
):
  def trace(
      ray: Ray,
      blockFoundFn: BlockRelWorld => Boolean
  ): Option[(BlockRelWorld, Option[Int])] =
    if blockFoundFn(camera.blockCoords) && blockTouched(ray, camera.blockCoords)
    then Some((camera.blockCoords, None))
    else traceIt(camera.blockCoords, ray, blockFoundFn, 1000)

  @tailrec
  private def traceIt(
      current: BlockRelWorld,
      ray: Ray,
      blockFoundFn: BlockRelWorld => Boolean,
      ttl: Int
  ): Option[(BlockRelWorld, Option[Int])] =
    if (ttl < 0) // TODO: this is a temporary fix for ray-loops
      return None

    val points = BlockState.vertices.map(v => asNormalCoords(current, v).toVector3d)

    val index = sideIndex(ray, points)
    val side = actualSide(ray, points, index)
    val normal = sideNormal(points, index, side)

    if ray.v.dot(normal) <= 0 then // TODO: this is a temporary fix for ray-loops
      val pointOnSide = points(if (side == 0) index else index + 6)
      val distance =
        Math.abs(pointOnSide.dot(normal) / ray.v.dot(normal)) // abs may be needed (a/-0)
      if distance <= maxDistance * CylinderSize.y60 then
        val hitBlockCoords = current.offset(NeighborOffsets(side))

        if blockFoundFn(hitBlockCoords) && blockTouched(ray, hitBlockCoords)
        then Some((hitBlockCoords, Some(side.oppositeSide)))
        else traceIt(hitBlockCoords, ray, blockFoundFn, ttl - 1)
      else None
    else
      System.err.println(
        "At least one bug has not been figured out yet! (Rayloops in RayTracer.trace.traceIt)"
      )
      None

  private def sideNormal(points: Seq[Vector3d], index: Int, side: Int) =
    val PA = new Vector3d
    val PB = new Vector3d

    if side == 0 then
      points((index + 1) % 6).sub(points(index), PA)
      points((index + 5) % 6).sub(points(index), PB)
    else if side == 1 then
      points((index + 5) % 6 + 6).sub(points(index + 6), PA)
      points((index + 1) % 6 + 6).sub(points(index + 6), PB)
    else
      points((index + 1) % 6 + 6).sub(points(index + 6), PA)
      points(index).sub(points(index + 6), PB)

    PA.cross(PB, new Vector3d())

  private def sideIndex(ray: Ray, points: Seq[Vector3d]) =
    if ray.toTheRight(points(0 + 6), points(0))
    then
      (5 to 1 by -1).find(index => !ray.toTheRight(points(index + 6), points(index))).getOrElse(0)
    else (1 to 5).find(index => ray.toTheRight(points(index + 6), points(index))).getOrElse(6) - 1

  private def actualSide(ray: Ray, points: Seq[Vector3d], index: Int) =
    if ray.toTheRight(points(index), points((index + 1) % 6))
    then 0
    else if !ray.toTheRight(points(index + 6), points((index + 1) % 6 + 6))
    then 1
    else index + 2

  extension (side: Int)
    private def oppositeSide = if (side < 2) 1 - side else (side - 2 + 3) % 6 + 2

  private def asNormalCoords(blockPos: BlockRelWorld, offset: CylCoords): NormalCoords =
    (BlockCoords(blockPos).toCylCoords + offset)
      .toNormalCoords(CylCoords(camera.view.position))

  private def blockTouched(ray: Ray, hitBlockCoords: BlockRelWorld): Boolean =
    world.getBlock(hitBlockCoords) match
      case block if block.blockType != Blocks.Air =>
        val points =
          for v <- block.blockType.bounds(block.metadata).vertices
          yield asNormalCoords(hitBlockCoords, v).toVector3d

        (0 until 8).exists(side => ray.intersectsPolygon(points, side))
      case _ => false

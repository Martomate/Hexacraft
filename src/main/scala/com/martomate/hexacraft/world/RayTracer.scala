package com.martomate.hexacraft.world

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.util.MathUtils.oppositeSide
import com.martomate.hexacraft.world.block.state.BlockState
import com.martomate.hexacraft.world.block.{Blocks, HexBox}
import com.martomate.hexacraft.world.camera.Camera
import com.martomate.hexacraft.world.coord.fp.{BlockCoords, CylCoords, NormalCoords}
import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, NeighborOffsets}
import org.joml.{Vector2fc, Vector3d, Vector3dc, Vector4f}

import scala.annotation.tailrec

object Ray:
  def fromScreen(camera: Camera, normalizedScreenCoords: Vector2fc): Option[Ray] =
    val coords = normalizedScreenCoords
    if coords.x < -1 || coords.x > 1 || coords.y < -1 || coords.y > 1
    then None
    else
      val coords4 = new Vector4f(coords.x, coords.y, -1, 1)
      coords4.mul(camera.proj.invMatrix)
      coords4.set(coords4.x, coords4.y, -1, 0)
      coords4.mul(camera.view.invMatrix)
      val ray = new Vector3d(coords4.x, coords4.y, coords4.z)
      ray.normalize()
      Some(Ray(ray))

class Ray(val v: Vector3d):

  /** @return
    *   true if the ray goes to the right of the line from `up` to `down` in a reference frame where
    *   `up` is directly above `down` (i.e. possibly rotated)
    */
  def toTheRight(down: Vector3d, up: Vector3d): Boolean = down.dot(up.cross(v, new Vector3d)) <= 0

  def intersectsPolygon(points: PointHexagon, side: Int): Boolean =
    val rightSeq =
      if side < 2 then
        for index <- 0 until 6
        yield
          val PA = if side == 0 then points.up(index) else points.down(index)
          val PB = if side == 0 then points.up((index + 1) % 6) else points.down((index + 1) % 6)

          this.toTheRight(PA, PB)
      else
        val order = Seq(0, 1, 3, 2)
        for index <- 0 until 4
        yield
          val aIdx = (order(index) % 2 + side - 2) % 6
          val PA = if order(index) / 2 == 0 then points.up(aIdx) else points.down(aIdx)

          val bIdx = (order((index + 1) % 4) % 2 + side - 2) % 6
          val PB = if order((index + 1) % 4) / 2 == 0 then points.up(bIdx) else points.down(bIdx)

          this.toTheRight(PA, PB)
    allElementsSame(rightSeq)

  private def allElementsSame(seq: IndexedSeq[Boolean]) = !seq.exists(_ != seq(0))

object PointHexagon:
  def fromHexBox(hexBox: HexBox, location: BlockRelWorld, camera: Camera)(implicit
      cylSize: CylinderSize
  ): PointHexagon =
    val points =
      for v <- hexBox.vertices
      yield asNormalCoords(location, v, camera).toVector3d
    new PointHexagon(points)

  private def asNormalCoords(blockPos: BlockRelWorld, offset: CylCoords, camera: Camera)(implicit
      cylSize: CylinderSize
  ): NormalCoords =
    val blockCoords = BlockCoords(blockPos).toCylCoords + offset
    blockCoords.toNormalCoords(CylCoords(camera.view.position))

class PointHexagon(points: Seq[Vector3d]):
  def up(idx: Int): Vector3d = points(idx)
  def down(idx: Int): Vector3d = points(idx + 6)

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

    val points = PointHexagon.fromHexBox(BlockState.boundingBox, current, camera)

    val index = sideIndex(ray, points)
    val side = actualSide(ray, points, index)
    val normal = sideNormal(points, index, side)

    if ray.v.dot(normal) <= 0 then // TODO: this is a temporary fix for ray-loops
      val pointOnSide =
        if side == 0 then points.up(index) else points.down(index)
      val distance =
        Math.abs(pointOnSide.dot(normal) / ray.v.dot(normal)) // abs may be needed (a/-0)
      if distance <= maxDistance * CylinderSize.y60 then
        val hitBlockCoords = current.offset(NeighborOffsets(side))

        if blockFoundFn(hitBlockCoords) && blockTouched(ray, hitBlockCoords)
        then Some((hitBlockCoords, Some(oppositeSide(side))))
        else traceIt(hitBlockCoords, ray, blockFoundFn, ttl - 1)
      else None
    else
      System.err.println(
        "At least one bug has not been figured out yet! (Rayloops in RayTracer.trace.traceIt)"
      )
      None

  private def sideNormal(points: PointHexagon, index: Int, side: Int) =
    val PA = new Vector3d
    val PB = new Vector3d

    if side == 0 then
      points.up((index + 1) % 6).sub(points.up(index), PA)
      points.up((index + 5) % 6).sub(points.up(index), PB)
    else if side == 1 then
      points.down((index + 5) % 6).sub(points.down(index), PA)
      points.down((index + 1) % 6).sub(points.down(index), PB)
    else
      points.down((index + 1) % 6).sub(points.down(index), PA)
      points.up(index).sub(points.down(index), PB)

    PA.cross(PB, new Vector3d())

  private def sideIndex(ray: Ray, points: PointHexagon) =
    if ray.toTheRight(points.down(0), points.up(0))
    then
      (5 to 1 by -1)
        .find(index => !ray.toTheRight(points.down(index), points.up(index)))
        .getOrElse(0)
    else
      (1 to 5).find(index => ray.toTheRight(points.down(index), points.up(index))).getOrElse(6) - 1

  private def actualSide(ray: Ray, points: PointHexagon, index: Int) =
    if ray.toTheRight(points.up(index), points.up((index + 1) % 6))
    then 0
    else if !ray.toTheRight(points.down(index), points.down((index + 1) % 6))
    then 1
    else index + 2

  private def blockTouched(ray: Ray, hitBlockCoords: BlockRelWorld): Boolean =
    world.getBlock(hitBlockCoords) match
      case block if block.blockType != Blocks.Air =>
        (0 until 8).exists(side => {
          val boundingBox = block.blockType.bounds(block.metadata)
          val points = PointHexagon.fromHexBox(boundingBox, hitBlockCoords, camera)
          ray.intersectsPolygon(points, side)
        })
      case _ => false

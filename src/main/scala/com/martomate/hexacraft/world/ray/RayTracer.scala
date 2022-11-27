package com.martomate.hexacraft.world.ray

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.util.MathUtils.oppositeSide
import com.martomate.hexacraft.world.BlocksInWorld
import com.martomate.hexacraft.world.block.{Blocks, BlockState, HexBox}
import com.martomate.hexacraft.world.camera.Camera
import com.martomate.hexacraft.world.coord.fp.{BlockCoords, CylCoords, NormalCoords}
import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, NeighborOffsets}

import org.joml.{Vector2fc, Vector3d, Vector3dc, Vector4f}
import scala.annotation.tailrec

class RayTracer(world: BlocksInWorld, camera: Camera, maxDistance: Double)(using
    cylSize: CylinderSize,
    Blocks: Blocks
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
    else (1 to 5).find(index => ray.toTheRight(points.down(index), points.up(index))).getOrElse(6) - 1

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

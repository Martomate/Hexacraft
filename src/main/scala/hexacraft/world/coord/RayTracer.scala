package hexacraft.world.coord

import hexacraft.Camera
import hexacraft.block.BlockState
import hexacraft.world.storage.World
import org.joml.{Vector2f, Vector3d, Vector4f}


class RayTracer(world: World, camera: Camera, maxDistance: Double) {
  private val ray = new Vector3d()
  private var rayValid = false

  def setRayFromScreen(normalizedScreenCoords: Vector2f): Unit = {
    val coords = normalizedScreenCoords
    if (coords.x < -1 || coords.x > 1 || coords.y < -1 || coords.y > 1) {
      rayValid = false
    } else {
      rayValid = true
      val coords4 = new Vector4f(coords.x, coords.y, -1, 1)
      coords4.mul(camera.invProjMatr)
      coords4.set(coords4.x, coords4.y, -1, 0)
      coords4.mul(camera.invViewMatr)
      ray.set(coords4.x, coords4.y, coords4.z)
      ray.normalize()
    }
  }

  def trace(blockFoundFn: BlockRelWorld => Boolean): Option[(BlockRelWorld, Option[Int])] = {
    def toTheRight(PA: Vector3d, PB: Vector3d): Boolean = PA.dot(PB.cross(ray, new Vector3d)) <= 0

    def blockTouched(hitBlockCoords: BlockRelWorld): Boolean = {
      world.getBlock(hitBlockCoords).map(b => b.blockType.bounds(b).vertices.map(v => CoordUtils.fromBlockCoords(world, camera, hitBlockCoords, v, new Vector3d))) match {
        case Some(points) =>
          val PA = new Vector3d
          val PB = new Vector3d

          (0 until 8).exists(side => {
            val seq = if (side < 2) {
              for (index <- 0 until 6) yield {
                PA set points(index + 6 * side)
                PB set points((index + 1) % 6 + 6 * side)

                toTheRight(PA, PB)
              }
            } else {
              val order = Seq(0, 1, 3, 2)
              for (index <- 0 until 4) yield {
                PA set points((order(index) % 2 + side - 2) % 6 + 6 * (order(index) / 2))
                PB set points((order((index + 1) % 4) % 2 + side - 2) % 6 + 6 * (order((index + 1) % 4) / 2))

                toTheRight(PA, PB)
              }
            }
            !seq.exists(_ != seq(0))
          })
        case None => false
      }
    }

    def traceIt(current: BlockRelWorld, blockFoundFn: BlockRelWorld => Boolean): Option[(BlockRelWorld, Option[Int])] = {
      val points = BlockState.vertices.map(v => CoordUtils.fromBlockCoords(world, camera, current, v, new Vector3d))

      val PA = new Vector3d(points(0 + 6))
      val PB = new Vector3d(points(0))

      val index = if (toTheRight(PA, PB)) {
        (5 to 1 by -1).find(index => !toTheRight(points(index + 6), points(index))).getOrElse(0)
      } else {
        (1 to 5).find(index => toTheRight(points(index + 6), points(index))).getOrElse(6) - 1
      }

      PA set points((index + 1) % 6)
      PB set points(index)

      val side = if (toTheRight(PA, PB)) { // under ceiling
        PA set points(index + 6)
        PB set points((index + 1) % 6 + 6)

        if (toTheRight(PA, PB)) index + 2 // over floor
        else 1
      } else 0

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

      val pointOnSide = points(if (side == 0) index else index + 6)
      val normal = PA.cross(PB, new Vector3d())

      if (ray.dot(normal) <= 0) { // TODO: this is a temporary fix for ray-loops
        val distance = Math.abs(pointOnSide.dot(normal) / ray.dot(normal)) // abs may be needed (a/-0)
        if (distance <= maxDistance * CoordUtils.y60) {
          val offsets = BlockState.neighborOffsets(side)
          val hitBlockCoords = BlockRelWorld(current.x + offsets._1, current.y + offsets._2, current.z + offsets._3, world.size)
          if (blockFoundFn(hitBlockCoords) && blockTouched(hitBlockCoords)) {
            val hoverSide = if (side < 2) 1 - side else (side + 1) % 6 + 2 // (side - 2 + 3) % 6 + 2
            Some(hitBlockCoords, Some(hoverSide))
          } else traceIt(hitBlockCoords, blockFoundFn)
        } else None
      } else {
        System.err.println("At least one bug has not been figured out yet! (Rayloops in RayTracer.trace.traceIt)")
        None
      }
    }

    if (!rayValid) None
    else if (blockFoundFn(camera.blockCoords) && blockTouched(camera.blockCoords)) Some(camera.blockCoords, None)
    else traceIt(camera.blockCoords, blockFoundFn)
  }
}

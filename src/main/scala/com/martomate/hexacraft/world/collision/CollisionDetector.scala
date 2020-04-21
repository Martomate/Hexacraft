package com.martomate.hexacraft.world.collision

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.{Blocks, HexBox}
import com.martomate.hexacraft.world.chunk.{ChunkCache, IChunk}
import com.martomate.hexacraft.world.coord.CoordUtils
import com.martomate.hexacraft.world.coord.fp.{BlockCoords, CylCoords, SkewCylCoords}
import com.martomate.hexacraft.world.coord.integer.{BlockRelWorld, ChunkRelWorld}
import com.martomate.hexacraft.world.worldlike.BlocksInWorld
import org.joml.Vector3d

class CollisionDetector(world: BlocksInWorld)(implicit cylSize: CylinderSize) {
  private val reflectionDirs = Array(
    ( 0, -1,  0),
    ( 0,  1,  0),
    (-1,  0,  0),
    ( 1,  0,  0),
    ( 0,  0, -1),
    ( 0,  0,  1),
    ( 1,  0, -1),
    (-1,  0,  1)
  )
  private val reflDirsCyl = reflectionDirs.map(d => new SkewCylCoords(d._1, d._2, d._3, false)(null).toCylCoords)

  private val chunkCache: ChunkCache = new ChunkCache(world)

  def getChunk(coords: ChunkRelWorld): Option[IChunk] = Option(chunkCache.getChunk(coords))

  def collides(box1: HexBox, pos1: SkewCylCoords, box2: HexBox, pos2: CylCoords): Boolean = {
    val (bc, fc) = CoordUtils.toBlockCoords(pos2.toBlockCoords)
    val skewCoord = new BlockCoords(bc.x + fc.x, bc.y + fc.y, bc.z + fc.z, false).toSkewCylCoords

    distanceToCollision(box1, pos1, new SkewCylCoords(0, 0, 0), box2, skewCoord)._1 == 0
  }

  /** pos and velocity should be CylCoords in vector form. Velocity is per tick. */
  def positionAndVelocityAfterCollision(box: HexBox, pos: Vector3d, velocity: Vector3d): (Vector3d, Vector3d) = {
    chunkCache.clearCache()

    var result = (pos, velocity)
    val parts = (velocity.length * 10).toInt + 1
    velocity.div(parts)
    for (_ <- 1 to parts) {
      result = _collides(box, result._1, velocity)
    }
    result._2.mul(parts)
    result
  }

  private def _collides(box1: HexBox, pos: Vector3d, velocity: Vector3d): (Vector3d, Vector3d) = {
    if (velocity.x != 0 || velocity.y != 0 || velocity.z != 0) {
      val (bc, fc) = CoordUtils.toBlockCoords(new CylCoords(pos.x + velocity.x, pos.y + velocity.y, pos.z + velocity.z, false).toBlockCoords)
      val skewCoords = new BlockCoords(bc.x + fc.x, bc.y + fc.y, bc.z + fc.z, false).toSkewCylCoords
      val skewVelocity = new CylCoords(velocity.x, velocity.y, velocity.z, false).toSkewCylCoords
      var maxDistTuple: (Double, Int) = (1d, -1)
      for (y <- math.floor((skewCoords.y + box1.bottom) * 2).toInt to math.floor((skewCoords.y + box1.top) * 2).toInt) {
        for (x <- -1 to 1) {
          for (z <- -1 to 1) {
            if (x * z != 1) {// corners
            val coords = BlockRelWorld(bc.x + x, y, bc.z + z)
              getChunk(coords.getChunkRelWorld) match {
                case Some(chunk) =>
                  val blockState = Some(chunk.getBlock(coords.getBlockRelChunk)).filter(_.blockType != Blocks.Air)
                  blockState.map(_.blockType).foreach(blockType => {
                    val dist = distanceToCollision(box1, skewCoords, skewVelocity,
                      blockType.bounds(blockState.get.metadata),
                      new BlockCoords(bc.x + x, y, bc.z + z, false).toSkewCylCoords)
                    if (dist._1 < maxDistTuple._1) {
                      maxDistTuple = dist
                    }
                  })
                case None =>
                  maxDistTuple = (0, -1)// Chunk isn't loaded, you're stuck (so that you don't fall into the void or something)
              }
            }
          }
        }
      }
      val (maxDist, reflectionDir) = maxDistTuple
      if (maxDist < 1d) {
        if (reflectionDir != -1) {
          val normal = reflDirsCyl(reflectionDir).toVector3d.normalize()
          val newPos = new Vector3d(pos).add(velocity.x * maxDist, velocity.y * maxDist, velocity.z * maxDist)
          val vel = new Vector3d(velocity).mul(1 - maxDist)
          val dot = vel.dot(normal)
          vel.sub(normal.mul(dot))
          val result = _collides(box1, newPos, vel)
          //val falseLen = result._2.length
          if (maxDist != 1) {
            result._2.mul(1 / (1 - maxDist))
          }
          result
        } else (pos, new Vector3d)
      } else (pos.add(velocity, new Vector3d), velocity)
    } else (pos, velocity)
  }

  private def distanceToCollision(box1: HexBox, pos: SkewCylCoords, velocity: SkewCylCoords, box2: HexBox, pos2: SkewCylCoords): (Double, Int) = {
    val (x1, y1, z1, x2, y2, z2) = (pos.x + 0.5 * pos.z, pos.y, pos.z + 0.5 * pos.x, pos2.x + 0.5 * pos2.z, pos2.y, pos2.z + 0.5 * pos2.x)
    val (r1, r2, b1, b2, t1, t2) = (box1.smallRadius, box2.smallRadius, box1.bottom, box2.bottom, box1.top, box2.top)
    val (vx, vy, vz) = (velocity.x + 0.5 * velocity.z, velocity.y, velocity.z + 0.5 * velocity.x)
    val distances = Array(
      y2 - y1 + t2 - b1,// (  y2    + t2) - (  y1    + b1),
      y1 - y2 + t1 - b2,// (  y1    + t1) - (  y2    + b2),
      (     x2 + r2) - (     x1 - r1),
      (     x1 + r1) - (     x2 - r2),
      (z2      + r2) - (z1      - r1),
      (z1      + r1) - (z2      - r2),
      (z2 - x2 + r2) - (z1 - x1 - r1),
      (z1 - x1 + r1) - (z2 - x2 - r2)
    )

    if (distances.forall(_ >= 0)) {
      val velDists = reflectionDirs.map(t => t._1 * vx + t._2 * vy + t._3 * vz)

      if (distances.indices.exists(i => distances(i) <= velDists(i))) {
        val zipped = for (i <- distances.indices) yield {
          val distBefore = ((distances(i) - velDists(i)) * 1e9).toLong / 1e9
          val t = (distBefore, velDists(i))
          val a = if (t._2 <= 0 || t._1 > 0) 1 else -t._1 / t._2
          (a, i)
        }
        val minInZip = zipped.minBy(_._1)
        (math.min(minInZip._1, 1), minInZip._2)
      } else (0, -1)
    } else (1, -1)
  }
}

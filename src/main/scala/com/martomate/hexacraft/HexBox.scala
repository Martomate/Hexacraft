package com.martomate.hexacraft

import com.martomate.hexacraft.block.BlockAir
import com.martomate.hexacraft.world.CylinderSize
import com.martomate.hexacraft.world.coord._
import com.martomate.hexacraft.world.storage.World
import org.joml.Vector3d

object HexBox {
  private val reflectionDirs = IndexedSeq(
      ( 0, -1,  0),
      ( 0,  1,  0),
      (-1,  0,  0),
      ( 1,  0,  0),
      ( 0,  0, -1),
      ( 0,  0,  1),
      ( 1,  0, -1),
      (-1,  0,  1)
  )
  private val reflDirsCyl = reflectionDirs.map(d => new SkewCylCoords(d._1, d._2, d._3, null, false).toCylCoords)
  
  def collides(box1: HexBox, pos1: SkewCylCoords, box2: HexBox, pos2: CylCoords): Boolean = {
    val (bc, fc) = CoordUtils.toBlockCoords(pos2.toBlockCoords)
    val skewCoord = new BlockCoords(bc.x + fc.x, bc.y + fc.y, bc.z + fc.z, pos1.cylSize, false).toSkewCylCoords
    
    box1.distanceToCollision(pos1, new SkewCylCoords(0, 0, 0, pos1.cylSize), box2, skewCoord)._1 == 0
  }
}

/** radius is the big radius of the hexagon */
class HexBox(val radius: Float, val bottom: Float, val top: Float) {
  val smallRadius: Double = radius * CylinderSize.y60

  def vertices: IndexedSeq[CylCoords] = {
    //val ints = Seq(1, 2, 0, 3, 5, 4)

    for {
      s <- 0 to 1
      i <- 0 until 6
    } yield {
      val v = i * Math.PI / 3
      val x = Math.cos(v).toFloat
      val z = Math.sin(v).toFloat
      new CylCoords(x * radius, (1 - s) * (top - bottom) + bottom, z * radius, null, false)
    }
  }

  /** pos and velocity should be CylCoords in vector form. Velocity is per tick. */
  def positionAndVelocityAfterCollision(pos: Vector3d, velocity: Vector3d, world: World): (Vector3d, Vector3d) = {
    var result = (pos, velocity)
    val parts = (velocity.length * 10).toInt + 1
    velocity.div(parts)
    for (_ <- 1 to parts) {
      result = _collides(result._1, velocity, world)
    }
    result._2.mul(parts)
    result
  }

  private def _collides(pos: Vector3d, velocity: Vector3d, world: World): (Vector3d, Vector3d) = {
    if (velocity.x != 0 || velocity.y != 0 || velocity.z != 0) {
      val (bc, fc) = CoordUtils.toBlockCoords(new CylCoords(pos.x + velocity.x, pos.y + velocity.y, pos.z + velocity.z, world.size, false).toBlockCoords)
      val skewCoords = new BlockCoords(bc.x + fc.x, bc.y + fc.y, bc.z + fc.z, world.size, false).toSkewCylCoords
      val skewVelocity = new CylCoords(velocity.x, velocity.y, velocity.z, world.size, false).toSkewCylCoords
      var maxDistTuple: (Double, Int) = (1d, -1)
      for (y <- math.floor((skewCoords.y + bottom) * 2).toInt to math.floor((skewCoords.y + top) * 2).toInt) {
        for (x <- -1 to 1) {
          for (z <- -1 to 1) {
            if (x * z != 1) {// corners
              val coords = BlockRelWorld(bc.x + x, y, bc.z + z, world.size)
              world.getChunk(coords.getChunkRelWorld) match {
                case Some(chunk) =>
                  val blockState = Some(chunk.getBlock(coords.getBlockRelChunk)).filter(_.blockType != BlockAir)
                  blockState.map(_.blockType).foreach(blockType => {
                    val dist = distanceToCollision(skewCoords, skewVelocity, blockType.bounds(blockState.get), new BlockCoords(bc.x + x, y, bc.z + z, world.size, false).toSkewCylCoords)
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
          val normal = HexBox.reflDirsCyl(reflectionDir).toVector3d.normalize()
          val newPos = new Vector3d(pos).add(velocity.x * maxDist, velocity.y * maxDist, velocity.z * maxDist)
          val vel = new Vector3d(velocity).mul(1 - maxDist)
          val dot = vel.dot(normal)
          vel.sub(normal.mul(dot))
          val result = _collides(newPos, vel, world)
          //val falseLen = result._2.length
          if (maxDist != 1) {
            result._2.mul(1 / (1 - maxDist))
          }
          result
        } else (pos, new Vector3d)
      } else (pos.add(velocity, new Vector3d), velocity)
    } else (pos, velocity)
  }
  
  private def distanceToCollision(pos: SkewCylCoords, velocity: SkewCylCoords, box2: HexBox, pos2: SkewCylCoords): (Double, Int) = {
    val (x1, y1, z1, x2, y2, z2) = (pos.x + 0.5 * pos.z, pos.y, pos.z + 0.5 * pos.x, pos2.x + 0.5 * pos2.z, pos2.y, pos2.z + 0.5 * pos2.x)
    val (r1, r2, b1, b2, t1, t2) = (smallRadius, box2.smallRadius, bottom, box2.bottom, top, box2.top)
    val (vx, vy, vz) = (velocity.x + 0.5 * velocity.z, velocity.y, velocity.z + 0.5 * velocity.x)
    val distances = Vector(
        y2 - y1 + t2 - b1,// (  y2    + t2) - (  y1    + b1),
        y1 - y2 + t1 - b2,// (  y1    + t1) - (  y2    + b2),
        (     x2 + r2) - (     x1 - r1),
        (     x1 + r1) - (     x2 - r2),
        (z2      + r2) - (z1      - r1),
        (z1      + r1) - (z2      - r2),
        (z2 - x2 + r2) - (z1 - x1 - r1),
        (z1 - x1 + r1) - (z2 - x2 - r2)
    )
    
    val velDists = HexBox.reflectionDirs.map(t => t._1 * vx + t._2 * vy + t._3 * vz)
    
    val distBefore = distances.indices.map(i => ((distances(i) - velDists(i)) * 1e9).toLong / 1e9)
    if (distances.min >= 0) {
      
      val zipped = distBefore.zip(velDists).map(t => if (t._2 <= 0 || t._1 > 0) 1 else -t._1 / t._2).zipWithIndex
      val distBeforeMin = distBefore.zipWithIndex.minBy(_._1)
      val minInZip = zipped.minBy(_._1)
      
      if (distBeforeMin._1 <= 0) (math.min(minInZip._1, 1), minInZip._2)
      else (0, -1)
    } else (1, -1)
  }
}
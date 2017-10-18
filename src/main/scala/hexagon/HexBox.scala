package hexagon

import hexagon.world.coord.CylCoord
import hexagon.world.storage.World
import hexagon.world.coord.CoordUtils
import hexagon.world.coord.BlockCoord
import hexagon.world.coord.BlockRelWorld
import hexagon.world.coord.SkewCylCoord
import org.joml.Vector3d
import java.text.NumberFormat

object HexBox {
  private val reflectionDirs = Vector(
      ( 0, -1,  0),
      ( 0,  1,  0),
      (-1,  0,  0),
      ( 1,  0,  0),
      ( 0,  0, -1),
      ( 0,  0,  1),
      ( 1,  0, -1),
      (-1,  0,  1)
  )
  private val reflDirsCyl = reflectionDirs.map(d => new SkewCylCoord(d._1, d._2, d._3, null, false).toCylCoord)
  
  def collides(box1: HexBox, pos1: SkewCylCoord, box2: HexBox, pos2: CylCoord): Boolean = {
    val (bc, fc) = CoordUtils.toBlockCoords(pos2.toBlockCoord)
    val skewCoord = new BlockCoord(bc.x + fc.x, bc.y + fc.y, bc.z + fc.z, pos1.world, false).toSkewCylCoord
    
    box1.distanceToCollision(pos1, new SkewCylCoord(0, 0, 0, pos1.world), box2, skewCoord)._1 == 0
  }
}

/** radius is the big radius of the hexagon */
class HexBox(val radius: Float, val bottom: Float, val top: Float) {
  val smallRadius = radius * CoordUtils.y60
  
  /** pos and velocity should be CylCoords in vector form. Velocity is per tick. */
  def afterCollision(pos: Vector3d, velocity: Vector3d, world: World): (Vector3d, Vector3d) = {
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
      val (bc, fc) = CoordUtils.toBlockCoords(new CylCoord(pos.x + velocity.x, pos.y + velocity.y, pos.z + velocity.z, world, false).toBlockCoord)
      val skewCoord = new BlockCoord(bc.x + fc.x, bc.y + fc.y, bc.z + fc.z, world, false).toSkewCylCoord
      val skewVelocity = new CylCoord(velocity.x, velocity.y, velocity.z, world, false).toSkewCylCoord
      var maxDistTuple: (Double, Int) = (1d, -1)
      for (y <- math.floor((skewCoord.y + bottom) * 2).toInt to math.floor((skewCoord.y + top) * 2).toInt) {
        for (x <- -1 to 1) {
          for (z <- -1 to 1) {
            if (x * z != 1) {// corners
              val coords = BlockRelWorld(bc.x + x, y, bc.z + z, world)
              world.getChunk(coords.getChunkRelWorld) match {
                case Some(chunk) =>
                  chunk.getBlock(coords.getBlockRelChunk).map(_.blockType).foreach(blockType => {
                    val dist = distanceToCollision(skewCoord, skewVelocity, blockType.bounds, new BlockCoord(bc.x + x, y, bc.z + z, world, false).toSkewCylCoord)
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
          val falseLen = result._2.length
          if (maxDist != 1) {
            result._2.mul(1 / (1 - maxDist))
          }
          result
        } else (pos, new Vector3d)
      } else (pos.add(velocity, new Vector3d), velocity)
    } else (pos, velocity)
  }
  
  private def distanceToCollision(pos: SkewCylCoord, velocity: SkewCylCoord, box2: HexBox, pos2: SkewCylCoord): (Double, Int) = {
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

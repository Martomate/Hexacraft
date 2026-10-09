package hexacraft.server.world

import hexacraft.server.entity.EntityModels
import hexacraft.world.{BlocksInWorld, CylinderSize}
import hexacraft.world.block.Block
import hexacraft.world.coord.{BlockCoords, BlockRelWorld, CoordUtils, CylCoords}
import hexacraft.world.entity.{EntityModel, EntityPart}

import org.joml.{Matrix4d, Vector3d, Vector3dc}

import java.util.concurrent.ConcurrentHashMap

/** A point inside an entity that stands for a small part of the entity's volume.
  *
  * @param offset
  *   the position of the point relative to the entity (before the entity is rotated), in world units
  * @param volume
  *   the volume that the point stands for
  * @param height
  *   the height of the region that the point stands for, used to make the submerged volume change smoothly
  */
case class VolumeSample(offset: Vector3dc, volume: Double, height: Double)

/** Points spread evenly through the volume of an entity, used to compute how much of the entity is under water.
  *
  * Each part of the model is sampled on its own, so the total volume is exact, and each point stands for an equal share
  * of its part's volume. The model is used in its resting pose.
  */
class VolumeSamples(val samples: IndexedSeq[VolumeSample]) {
  val totalVolume: Double = samples.map(_.volume).sum

  /** The volume of the entity that is under water, given the entity's position and rotation */
  def volumeInWater(world: BlocksInWorld, position: CylCoords, rotation: Vector3dc)(using CylinderSize): Double = {
    val rotationMatrix = new Matrix4d().rotateZ(rotation.z).rotateX(rotation.x).rotateY(rotation.y)
    val offset = new Vector3d

    var result = 0.0
    for s <- samples do {
      rotationMatrix.transformPosition(s.offset, offset)
      val point = position.offset(offset.x, offset.y, offset.z)
      result += s.volume * VolumeSamples.fractionInWater(world, point, s.height)
    }
    result
  }
}

object VolumeSamples {

  /** The approximate distance between the samples, in model pixels */
  val DefaultSpacing: Double = 8

  val empty: VolumeSamples = VolumeSamples(IndexedSeq.empty)

  private val samplesByType = new ConcurrentHashMap[String, VolumeSamples]()

  /** The samples of the model of the given entity type (computed once per type), or None if there is no model */
  def forType(entityType: String): Option[VolumeSamples] = {
    EntityModels.forType(entityType).map(model => samplesByType.computeIfAbsent(entityType, _ => fromModel(model)))
  }

  def fromModel(model: EntityModel, spacing: Double = DefaultSpacing): VolumeSamples = {
    val px = EntityModel.pixelSize
    val parts = model.parts
    val partTransforms = new Array[Matrix4d](parts.size)
    val samples = IndexedSeq.newBuilder[VolumeSample]

    for idx <- parts.indices do {
      val part = parts(idx)
      val parentIdx = model.parentIndices(idx)

      // This must match how the client places the parts (in EntityPose) when it renders the model
      partTransforms(idx) = (if parentIdx != -1 then Matrix4d(partTransforms(parentIdx)) else Matrix4d())
        .translate(part.position.x * px, part.position.y * px, part.position.z * px)
        .rotateZ(part.rotation.z)
        .rotateX(part.rotation.x)
        .rotateY(part.rotation.y)

      if part.isVisible then {
        samples ++= samplePrism(part, partTransforms(idx), spacing)
      }
    }

    VolumeSamples(samples.result())
  }

  /** Samples the prism of the part, which is a hexagon along the y-axis.
    *
    * The hexagon is split into its 6 triangles, which are split into m * m smaller triangles, and the points are placed
    * at the centers of those. This is repeated in n layers along the length of the prism.
    */
  private def samplePrism(part: EntityPart, transform: Matrix4d, spacing: Double): Seq[VolumeSample] = {
    val px = EntityModel.pixelSize
    val radius = part.prism.radius.toDouble
    val length = part.prism.length.toDouble

    val m = math.max(1, math.ceil(radius / spacing).toInt)
    val n = math.max(1, math.ceil(length / spacing).toInt)

    val volume = 1.5 * math.sqrt(3) * radius * radius * length * px * px * px
    val volumePerSample = volume / (6 * m * m * n)
    val height = math.cbrt(volumePerSample)

    // The points in one layer. The hexagon consists of 6 triangles, each between the center (the origin) and two
    // neighboring corners (v0 and v1). A point in such a triangle is a * v0 + b * v1, where a, b >= 0 and a + b <= 1.
    //
    // Both a and b are divided into m steps, and (i, j) is the number of steps towards v0 and v1. Each cell (i, j) is
    // split by its diagonal into a lower left half (up) and an upper right half (down). The upper right half only fits
    // in the triangle if i + j <= m - 2. Each point is at the center of a half, and all halves have the same area.
    // This is what it looks like for m = 2 (drawn with a and b as perpendicular axes):
    //
    //    b
    //    1 +
    //      |\
    //      |  \
    //      | up \
    //    ½ +-----+
    //      |\down|\
    //      |  \  |  \
    //      | up \| up \
    //    0 +-----+-----+ a
    //      0     ½     1
    //       i = 0  i = 1
    //
    val layerPoints = for {
      corner <- 0 until 6
      v0x = radius * math.cos(corner * math.Pi / 3)
      v0z = radius * math.sin(corner * math.Pi / 3)
      v1x = radius * math.cos((corner + 1) * math.Pi / 3)
      v1z = radius * math.sin((corner + 1) * math.Pi / 3)
      i <- 0 until m
      j <- 0 until m - i
      (a, b) <- {
        val up = Seq(((i + 1.0 / 3) / m, (j + 1.0 / 3) / m))
        val down = if i + j <= m - 2 then Seq(((i + 2.0 / 3) / m, (j + 2.0 / 3) / m)) else Seq()
        up ++ down
      }
    } yield (a * v0x + b * v1x, a * v0z + b * v1z)

    for {
      layer <- 0 until n
      y = part.prismOffset + (layer + 0.5) * length / n
      (x, z) <- layerPoints
    } yield {
      val offset = transform.transformPosition(new Vector3d(x * px, y * px, z * px))
      VolumeSample(offset, volumePerSample, height)
    }
  }

  /** How much (between 0 and 1) of a region of the given height, centered at the point, is under water */
  private def fractionInWater(world: BlocksInWorld, point: CylCoords, height: Double)(using CylinderSize): Double = {
    waterSurfaceAt(world, point) match {
      case Some(surface) =>
        val bottom = point.y - height / 2
        math.max(0, math.min(1, (surface - bottom) / height))
      case None => 0
    }
  }

  /** The height of the water surface at the point, if there is water in the block of the point or right below it */
  private def waterSurfaceAt(world: BlocksInWorld, point: CylCoords)(using CylinderSize): Option[Double] = {
    val block = CoordUtils.getEnclosingBlock(point.toBlockCoords)._1

    waterTop(world, block) match {
      case Some(top) =>
        val isFull = top >= blockBottom(block) + 0.5 - 1e-6
        if isFull && waterTop(world, block.offset(0, 1, 0)).isDefined then {
          Some(Double.PositiveInfinity) // the water continues above this block
        } else {
          Some(top)
        }
      case None =>
        waterTop(world, block.offset(0, -1, 0)) // the lower part of the region might reach into the water below
    }
  }

  /** The height of the water surface in the block, if it's a water block */
  private def waterTop(world: BlocksInWorld, block: BlockRelWorld)(using CylinderSize): Option[Double] = {
    val state = world.getBlock(block)
    if state.blockType == Block.Water then {
      Some(blockBottom(block) + state.blockType.bounds(state.metadata).top)
    } else {
      None
    }
  }

  private def blockBottom(block: BlockRelWorld)(using CylinderSize): Double = BlockCoords(block).toCylCoords.y
}

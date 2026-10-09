package hexacraft.client

import hexacraft.client.entity.ModelComponent
import hexacraft.world.{Camera, CylinderSize, PointHexagon, Ray}
import hexacraft.world.coord.CylCoords
import hexacraft.world.entity.Entity

import org.joml.{Vector3d, Vector3dc, Vector3f}

object EntityPicking {

  /** No entity model reaches further than this from the entity's position, so entities that are further away than
    * this (plus the max distance) can't be hit and are not tested
    */
  private val MaxModelSize = 4.0

  /** The distance along the ray to the closest entity it hits within `maxDistance`, if any */
  def closestEntityDistance(entities: Iterable[Entity], camera: Camera, ray: Ray, maxDistance: Double)(using
      CylinderSize
  ): Option[Double] = {
    val cameraPosition = CylCoords(camera.view.position)

    entities
      .filter(e => e.transform.position.toNormalCoords(cameraPosition).toVector3d.length < maxDistance + MaxModelSize)
      .flatMap(e => distanceToEntity(e, camera, ray))
      .filter(_ < maxDistance)
      .minOption
  }

  /** The distance along the ray to the closest point where it hits the entity, or None if it doesn't hit the entity.
    *
    * Each visible part of the entity's model is tested, in its current pose. Entities without a model are tested using
    * their bounding box (like the server does).
    */
  def distanceToEntity(entity: Entity, camera: Camera, ray: Ray)(using CylinderSize): Option[Double] = {
    entity.accessComponent { case c: ModelComponent => c } match {
      case Some(component) =>
        val parts = component.model.parts
        val prismTransforms = component.pose.prismTransforms

        parts.indices
          .filter(idx => parts(idx).isVisible)
          .flatMap { idx =>
            // The client renders a unit prism (radius 1, from y = 0 to y = 1) transformed by the prism transform
            def ring(y: Float): Seq[Vector3dc] = (0 until 6).map { i =>
              val angle = i * math.Pi / 3
              val corner = prismTransforms(idx).transformPosition(
                new Vector3f(math.cos(angle).toFloat, y, math.sin(angle).toFloat)
              )
              new Vector3d(corner)
            }

            val transform = entity.transform
            PointHexagon
              .fromEntityPrism(ring(1), ring(0), transform.position, transform.rotation, camera)
              .distanceToBox(ray)
          }
          .minOption
      case None =>
        val points = PointHexagon.fromHexBox(entity.boundingBox, entity.transform.position.toBlockCoords, camera)
        points.distanceToBox(ray)
    }
  }
}

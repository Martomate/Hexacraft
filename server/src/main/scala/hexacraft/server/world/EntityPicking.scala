package hexacraft.server.world

import hexacraft.world.{Camera, CylinderSize, PointHexagon, Ray}
import hexacraft.world.coord.CylCoords
import hexacraft.world.entity.Entity

import org.joml.{Matrix4d, Vector3d, Vector3dc}

object EntityPicking {

  /** The distance along the ray to the closest point where it hits the entity, or None if it doesn't hit the entity.
    *
    * Each visible part of the entity's model is tested (in the model's resting pose). Entities without a model are
    * tested using their bounding box.
    */
  def distanceToEntity(entity: Entity, camera: Camera, ray: Ray)(using CylinderSize): Option[Double] = {
    PlacedModel.forType(entity.typeName) match {
      case Some(placedModel) =>
        distanceToModel(placedModel, entity.transform.position, entity.transform.rotation, camera, ray)
      case None =>
        val points = PointHexagon.fromHexBox(entity.boundingBox, entity.transform.position.toBlockCoords, camera)
        points.distanceToBox(ray)
    }
  }

  /** The distance along the ray to the closest part of the model, placed at the given position and rotation */
  def distanceToModel(
      placedModel: PlacedModel,
      position: CylCoords,
      rotation: Vector3dc,
      camera: Camera,
      ray: Ray
  )(using CylinderSize): Option[Double] = {
    val rotationMatrix = new Matrix4d().rotateZ(rotation.z).rotateX(rotation.x).rotateY(rotation.y)
    val cameraPosition = CylCoords(camera.view.position)

    // The ray starts at the camera, so the corners are converted to coordinates relative to the camera
    def relativeToCamera(corner: Vector3dc): Vector3d = {
      val offset = rotationMatrix.transformPosition(corner, new Vector3d)
      position.offset(offset).toNormalCoords(cameraPosition).toVector3d
    }

    placedModel.prismCorners.flatMap { corners =>
      val prism = new PointHexagon(
        corners.top.map(relativeToCamera).toArray,
        corners.bottom.map(relativeToCamera).toArray
      )
      prism.distanceToBox(ray)
    }.minOption
  }
}

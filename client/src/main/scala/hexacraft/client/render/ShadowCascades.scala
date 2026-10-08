package hexacraft.client.render

import org.joml.{Matrix4f, Matrix4fc, Vector3dc, Vector3f, Vector3fc}

/** Cascaded shadow maps for the sun.
  *
  * The view frustum is split into slices along the view direction, and each slice gets its own orthographic shadow
  * map. The shadow maps are fitted to the bounding sphere of the slice rather than the slice itself, which means that
  * their size does not change when the camera rotates. Combined with snapping to the texel grid this keeps the shadow
  * edges from flickering when the camera moves.
  *
  * Note: everything is in the (camera-relative) coordinate system that the world is rendered in after being bent
  * around the cylinder. Near the camera this is approximately `worldCoords - cameraPosition`, which is what makes the
  * texel snapping work.
  */
object ShadowCascades {

  /** The distance from the camera (along the view direction) where each cascade ends */
  val splits: IndexedSeq[Float] = IndexedSeq(12f, 40f, 140f)

  /** The width and height of each shadow map */
  val resolution: Int = 2048

  /** @param matrix
    *   transforms a position into the clip space of the shadow map
    * @param texelSize
    *   the width of a texel in the shadow map (in world units)
    */
  case class Cascade(matrix: Matrix4f, texelSize: Float)

  /** @param invViewMatrix
    *   the inverse view matrix of the camera
    * @param projMatrix
    *   the projection matrix of the camera (only used to find the field of view)
    * @param cameraPosition
    *   the position of the camera in world coordinates (used for texel snapping)
    * @param sunDirection
    *   the direction towards the sun
    */
  def compute(
      invViewMatrix: Matrix4fc,
      projMatrix: Matrix4fc,
      cameraPosition: Vector3dc,
      sunDirection: Vector3fc,
      splits: IndexedSeq[Float] = splits,
      resolution: Int = resolution
  ): IndexedSeq[Cascade] = {
    val lightView = lightViewMatrix(sunDirection)

    // How far the frustum reaches sideways (diagonally) per unit along the view direction
    val tanHalfFovX = 1.0 / projMatrix.m00()
    val tanHalfFovY = 1.0 / projMatrix.m11()
    val diagonalSq = tanHalfFovX * tanHalfFovX + tanHalfFovY * tanHalfFovY

    // The camera position projected onto the horizontal and vertical axes of the shadow map
    val cameraLightX = cameraPosition.x * lightView.m00() + cameraPosition.y * lightView.m10() +
      cameraPosition.z * lightView.m20()
    val cameraLightY = cameraPosition.x * lightView.m01() + cameraPosition.y * lightView.m11() +
      cameraPosition.z * lightView.m21()

    for i <- splits.indices yield {
      val near = if i == 0 then 0.0 else splits(i - 1).toDouble
      val far = splits(i).toDouble

      val (centerDistance, radius) = boundingSphere(near, far, diagonalSq)

      val center = invViewMatrix.transformPosition(new Vector3f(0, 0, -centerDistance.toFloat))
      lightView.transformPosition(center)

      val texelSize = 2 * radius / resolution
      val left = snapToTexelGrid(center.x - radius, cameraLightX, texelSize)
      val bottom = snapToTexelGrid(center.y - radius, cameraLightY, texelSize)

      // The light looks along -z, so things closer to the sun have a larger z. The extra space towards the sun is
      // mostly for depth precision since depth clamping takes care of shadow casters beyond the near plane.
      val zNear = -(center.z + 2 * radius)
      val zFar = -(center.z - radius)

      val matrix = new Matrix4f()
        .setOrtho(
          left.toFloat,
          (left + 2 * radius).toFloat,
          bottom.toFloat,
          (bottom + 2 * radius).toFloat,
          zNear.toFloat,
          zFar.toFloat
        )
        .mul(lightView)

      Cascade(matrix, texelSize.toFloat)
    }
  }

  /** A rotation that makes the sun look along -z */
  private def lightViewMatrix(sunDirection: Vector3fc): Matrix4f = {
    val dir = new Vector3f(sunDirection).normalize()
    val up = if math.abs(dir.x) < 0.9f then new Vector3f(1, 0, 0) else new Vector3f(0, 1, 0)
    new Matrix4f().setLookAlong(-dir.x, -dir.y, -dir.z, up.x, up.y, up.z)
  }

  /** Returns the distance to the center of the smallest sphere containing the frustum slice between `near` and `far`,
    * and the radius of that sphere. The center is always on the view axis.
    */
  private[render] def boundingSphere(near: Double, far: Double, diagonalSq: Double): (Double, Double) = {
    // The center is chosen such that the near and far corners are equally far away from it
    val center = math.min((far + near) * (1 + diagonalSq) / 2, far)
    val radius = math.sqrt((far - center) * (far - center) + far * far * diagonalSq)
    (center, radius)
  }

  /** Moves `edge` (relative to the camera) so that it lies on a texel boundary in world space. The world space grid is
    * used (rather than the camera-relative one) so that the shadows stay still when the camera moves.
    */
  private def snapToTexelGrid(edge: Double, cameraOffset: Double, texelSize: Double): Double = {
    val offset = cameraOffset - math.floor(cameraOffset / texelSize) * texelSize
    math.floor((edge + offset) / texelSize) * texelSize - offset
  }
}

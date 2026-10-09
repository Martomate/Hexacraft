package hexacraft.server.entity

import hexacraft.physics.Density
import hexacraft.server.world.VolumeSamples

/** The masses of the entity types, calculated from the volume of their models and the density of their materials */
object EntityMasses {
  private val densitiesByType: Map[String, Density] = Map(
    "sheep" -> Density.fromSI(800),
    "boat" -> Density.fromSI(400)
  )

  /** The mass in kg, or None if the entity type has no model or no density */
  def forType(entityType: String): Option[Double] = {
    for {
      density <- densitiesByType.get(entityType)
      volume <- VolumeSamples.forType(entityType)
    } yield volume.totalVolume * density.toSI
  }
}

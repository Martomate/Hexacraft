package hexacraft.client.entity

import hexacraft.world.entity.EntityPart

/** Describes how the parts of an entity model are textured.
  *
  * Each part refers to a region of the texture, starting at the given offset (in texture pixels). The size of the
  * region is determined by the size of the part. Parts that are not mentioned use `defaultOffset`.
  */
class EntitySkin(
    val textureName: String,
    partTextureOffsets: Map[String, (Int, Int)],
    defaultOffset: (Int, Int) = (0, 0)
) {
  def partNames: Iterable[String] = partTextureOffsets.keys

  def texture(side: Int): Int = {
    val offset = if side < 2 then 0x12345 else 0
    val texID = 4
    offset << 12 | texID
  }

  def textureOffset(part: EntityPart, side: Int): (Int, Int) = {
    val (dx, dy) = side match {
      case 0 => (0, 0)
      case 1 => (0, EntitySkin.radiusInPixels(part) + EntitySkin.heightInPixels(part))
      case _ => ((side - 2) * EntitySkin.radiusInPixels(part), EntitySkin.radiusInPixels(part))
    }

    val (sx, sy) = partTextureOffsets.getOrElse(part.name, defaultOffset)
    (sx + dx, sy + dy)
  }

  def textureSize(part: EntityPart, side: Int): (Int, Int) = {
    if side < 2 then {
      (EntitySkin.radiusInPixels(part), EntitySkin.radiusInPixels(part))
    } else {
      (EntitySkin.radiusInPixels(part), EntitySkin.heightInPixels(part))
    }
  }
}

object EntitySkin {
  private def radiusInPixels(part: EntityPart): Int = part.prism.radius
  private def heightInPixels(part: EntityPart): Int = part.prism.length
}

/** The skins of all entity types. The parts are referred to by the names used in the models sent by the server. */
object EntitySkins {
  val player: EntitySkin = EntitySkin(
    "player",
    Map(
      "head" -> (0, 176),
      "leftBodyHalf" -> (0, 120),
      "rightBodyHalf" -> (48, 120),
      "rightArm" -> (48, 64),
      "leftArm" -> (0, 64),
      "rightLeg" -> (48, 0),
      "leftLeg" -> (0, 0)
    )
  )

  val sheep: EntitySkin = EntitySkin(
    "sheep",
    Map(
      "head" -> (0, 168),
      "body" -> (0, 88),
      "frontRightLeg" -> (36, 44),
      "frontLeftLeg" -> (0, 44),
      "backRightLeg" -> (36, 0),
      "backLeftLeg" -> (0, 0)
    )
  )

  val boat: EntitySkin = EntitySkin("boat", Map.empty) // all parts use the same part of the texture
}

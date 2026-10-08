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

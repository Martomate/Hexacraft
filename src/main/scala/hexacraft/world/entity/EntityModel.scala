package hexacraft.world.entity

import hexacraft.renderer.TextureSingle

trait EntityModel {
  def parts: Seq[EntityPart]

  def texture: TextureSingle

  def tick(): Unit
}

package com.martomate.hexacraft.world.entity

import com.martomate.hexacraft.renderer.TextureSingle

trait EntityModel {
  def parts: Seq[EntityPart]

  def texture: TextureSingle

  def tick(): Unit
}

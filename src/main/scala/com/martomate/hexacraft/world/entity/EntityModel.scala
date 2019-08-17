package com.martomate.hexacraft.world.entity

import com.martomate.hexacraft.resource.TextureSingle

trait EntityModel {
  def parts: Seq[EntityPart]

  def texture: TextureSingle
  def texSize: Int

  def tick(): Unit
}

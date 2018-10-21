package com.martomate.hexacraft.world.entity

trait EntityModel {
  def parts: Seq[EntityPart]

  def texSize: Int = 32

  def tick(): Unit
}

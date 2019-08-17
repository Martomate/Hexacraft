package com.martomate.hexacraft.world.settings

case class WorldSettings(name: Option[String],
                         size: Option[Byte],
                         seed: Option[Long])

object WorldSettings {
  def none = WorldSettings(None, None, None)
}
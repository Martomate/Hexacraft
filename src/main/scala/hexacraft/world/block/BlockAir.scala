package hexacraft.world.block

import hexacraft.physics.Viscosity

class BlockAir extends Block(0, "air", "Air") {
  override def canBeRendered: Boolean = false
  override def isCovering(metadata: Byte, side: Int): Boolean = false
  override def isTransmissive: Boolean = true
  override def isSolid: Boolean = false
  override def viscosity: Viscosity = Viscosity.air
}

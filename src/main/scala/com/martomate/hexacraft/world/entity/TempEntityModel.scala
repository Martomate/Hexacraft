package com.martomate.hexacraft.world.entity

import com.martomate.hexacraft.world.block.HexBox
import com.martomate.hexacraft.world.coord.fp.CylCoords

class TempEntityModel(pos: CylCoords, box: HexBox) extends EntityModel {
  private val theBox = new TempEntityPart(pos, box)

  override val parts: Seq[EntityPart] = Seq(theBox)
}

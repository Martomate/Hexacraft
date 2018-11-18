package com.martomate.hexacraft.world.entity.base

import com.martomate.hexacraft.resource.TextureSingle
import com.martomate.hexacraft.world.block.HexBox
import com.martomate.hexacraft.world.coord.fp.CylCoords
import com.martomate.hexacraft.world.entity.{EntityModel, EntityPart}
import org.joml.Vector3f

class BasicEntityModel(pos: CylCoords, box: HexBox) extends EntityModel {
  private val theBox = new BasicEntityPart(box, pos, new Vector3f)

  override val parts: Seq[EntityPart] = Seq(theBox)

  override def tick(): Unit = ()

  override def texture: TextureSingle = ???

  override def texSize: Int = ???
}

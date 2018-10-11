package com.martomate.hexacraft.world.entity

import com.martomate.hexacraft.util.CylinderSize
import com.martomate.hexacraft.world.block.HexBox
import com.martomate.hexacraft.world.coord.fp.{BlockCoords, CylCoords}
import org.joml.Vector3f

class TempEntityModel(pos: CylCoords, box: HexBox) extends EntityModel {
  private val theBox = new TempEntityPart(box, pos, new Vector3f)

  override val parts: Seq[EntityPart] = Seq(theBox)
}

class TempComplexEntityModel(pos: CylCoords, box: HexBox) extends EntityModel {
  import pos.cylSize.impl

  private val box1 = new TempEntityPart(
    new HexBox(0.5f, 0, 0.25f),
    BlockCoords(0, 1, 0).toCylCoords,
    new Vector3f(0, 0, 0)
  )
  private val box2 = new TempEntityPart(
    new HexBox(0.125f, 0, 0.5f),
    CylCoords(0, 0.5f, CylinderSize.y60.toFloat / 2),
    new Vector3f(-math.Pi.toFloat * 7 / 6, 0, 0)
  )
  private val box3 = new TempEntityPart(
    new HexBox(0.125f, 0, 0.5f),
    CylCoords(0, 0.5f, -CylinderSize.y60.toFloat / 2),
    new Vector3f(math.Pi.toFloat * 7 / 6, 0, 0)
  )
  private val box4 = new TempEntityPart(
    new HexBox(0.25f, 0, 0.5f),
    BlockCoords(0, 0, 0).toCylCoords,
    new Vector3f(0, 0, 0)
  )

  override val parts: Seq[EntityPart] = Seq(box1, box2, box3, box4)
}

package hexacraft.world.entity.base

import hexacraft.renderer.TextureSingle
import hexacraft.world.block.HexBox
import hexacraft.world.coord.fp.CylCoords
import hexacraft.world.entity.{EntityModel, EntityPart}
import org.joml.Vector3f

object BasicEntityModel:
  def create(pos: CylCoords.Offset, box: HexBox): BasicEntityModel =
    new BasicEntityModel(pos, box)

class BasicEntityModel(pos: CylCoords.Offset, box: HexBox) extends EntityModel {
  private val theBox = new BasicEntityPart(box, pos, new Vector3f)

  override val parts: Seq[EntityPart] = Seq(theBox)

  override def tick(): Unit = ()

  override def texture: TextureSingle = ???
}

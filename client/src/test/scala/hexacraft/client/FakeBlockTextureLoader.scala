package hexacraft.client

import hexacraft.renderer.PixelArray

class FakeBlockTextureLoader extends BlockTextureLoader:
  override def load(squareTextureNames: Seq[String], triTextureNames: Seq[String]): BlockTextureLoader.LoadedImages =
    BlockTextureLoader.LoadedImages(Seq(PixelArray(Array.fill(32 * 32)(0), false)), Map.empty.withDefault(_ => 0))

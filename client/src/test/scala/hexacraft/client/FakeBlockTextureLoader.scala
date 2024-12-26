package hexacraft.client

import hexacraft.renderer.PixelArray
import hexacraft.util.Result

import java.io.FileNotFoundException

class FakeBlockTextureLoader extends BlockTextureLoader:
  override def load(
      squareTextureNames: Seq[String],
      triTextureNames: Seq[String]
  ): Result[BlockTextureLoader.LoadedImages, FileNotFoundException] =
    Result.Ok(
      BlockTextureLoader.LoadedImages(Seq(PixelArray(Array.fill(32 * 32)(0), false)), Map.empty.withDefault(_ => 0))
    )

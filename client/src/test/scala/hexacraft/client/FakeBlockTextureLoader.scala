package hexacraft.client

import hexacraft.util.Result

class FakeBlockTextureLoader extends BlockTextureLoader {
  override def load(fileName: String): Result[Seq[Array[Int]], Throwable] = {
    Result.Ok(Seq(Array.fill(32 * 32)(0)))
  }
}

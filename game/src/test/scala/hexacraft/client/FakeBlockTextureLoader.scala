package hexacraft.client

class FakeBlockTextureLoader extends BlockTextureLoader:
  override def load(squareTextureNames: Seq[String], triTextureNames: Seq[String]): BlockTextureLoader.LoadedImages =
    BlockTextureLoader.LoadedImages(Seq(), Map.empty.withDefault(_ => 0))

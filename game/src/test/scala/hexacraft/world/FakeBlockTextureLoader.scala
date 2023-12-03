package hexacraft.world

import hexacraft.infra.fs.{BlockTextureLoader, BlockTextureMapping}

class FakeBlockTextureLoader extends BlockTextureLoader:
  override def reload(): BlockTextureMapping = BlockTextureMapping(Seq(), Map.empty.withDefault(_ => 0))

  override def textureMapping: BlockTextureMapping = BlockTextureMapping(Seq(), Map.empty.withDefault(_ => 0))

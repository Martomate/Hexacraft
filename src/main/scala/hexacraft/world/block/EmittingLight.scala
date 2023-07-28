package hexacraft.world.block

trait EmittingLight extends Block {
  override def lightEmitted: Byte = 14
}

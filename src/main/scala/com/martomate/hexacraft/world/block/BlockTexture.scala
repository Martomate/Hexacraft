package com.martomate.hexacraft.world.block

import com.martomate.hexacraft.util.Resource

object BlockTexture {
  val blockTextureSize: Int = 32
}

class BlockTexture(name: String)(using blockLoader: BlockLoader) extends Resource {
  private var _indices: Seq[Int] = _

  /** @return `(offsets << 12 | texture_array_index)` for each side */
  def indices: Seq[Int] = _indices

  load()

  private def load(): Unit = {
    _indices = blockLoader.loadBlockType(name)
  }

  def reload(): Unit =
    load() // TODO: this does not work. Make the new resource-loading-system instead of fixing this here

  def unload(): Unit = ()
}

package com.martomate.hexacraft.world.block

import com.martomate.hexacraft.resource.Resource

object BlockTexture {
  val blockTextureSize: Int = 32
}

class BlockTexture(name: String, blockLoader: IBlockLoader) extends Resource {
  private var _indices: Seq[Int] = _
  def indices: Seq[Int] = _indices

  load()

  def load(): Unit = {
    _indices = blockLoader.loadBlockType(name)
  }

  def reload(): Unit =
    load() // TODO: this does not work. Make the new resource-loading-system instead of fixing this here

  def unload(): Unit = ()
}

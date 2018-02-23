package hexagon.block

import hexagon.resource.Resource

object BlockTexture {
  val blockTextureSize: Int = 32
}

class BlockTexture(name: String) extends Resource {
  private var _indices: Seq[Int] = _
  def indices: Seq[Int] = _indices
  
  load()

  def load(): Unit = {
    _indices = BlockLoader.loadBlockType(name)
  }
  
  def reload(): Unit = load()// TODO: this does not work. Make the new resource-loading-system instead of fixing this here
  
  def unload(): Unit = ()
}

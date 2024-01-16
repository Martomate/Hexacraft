package hexacraft.util

class ResourceWrapper[T](make: () => T) {
  private val elem: T = make()
  def get: T = elem
}

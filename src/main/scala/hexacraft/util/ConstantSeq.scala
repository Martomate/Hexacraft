package hexacraft.util

class ConstantSeq[T](_size: Int, val default: T) extends IndexedSeq[T] {
  require(_size >= 0, "Size cannot be negative")

  def length: Int = _size
  def apply(index: Int): T = default

  override def iterator: Iterator[T] = new Iterator[T] {
    var idx = 0

    override def hasNext: Boolean = idx < _size
    override def next(): T = {
      idx += 1
      default
    }
  }
}

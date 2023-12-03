package hexacraft.math

extension (xs: Range) {
  def offset(dx: Int): Range =
    val (start, end, step) = (xs.start + dx, xs.end + dx, xs.step)
    if xs.isInclusive then start to end by step else start until end by step
}

case class Range2D(xs: Range, ys: Range) {
  def offset(dx: Int, dy: Int): Range2D =
    Range2D(xs.offset(dx), ys.offset(dy))
}

case class Range3D(xs: Range, ys: Range, zs: Range) {
  def offset(dx: Int, dy: Int, dz: Int): Range3D =
    Range3D(xs.offset(dx), ys.offset(dy), zs.offset(dz))
}

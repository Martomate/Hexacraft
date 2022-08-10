package com.martomate.hexacraft.world.coord.integer

case class Offset(dx: Int, dy: Int, dz: Int) {
  def +(other: Offset): Offset = Offset(dx + other.dx, dy + other.dy, dz + other.dz)
  def -(other: Offset): Offset = Offset(dx - other.dx, dy - other.dy, dz - other.dz)

  def manhattanDistance: Int =
    math.abs(dy) + math.max(math.max(math.abs(dx), math.abs(dz)), math.abs(dx + dz))
}

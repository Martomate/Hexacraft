package hexacraft.math.bits

opaque type Int20 <: AnyVal = Int

object Int20 {
  inline def apply(inline i: Int): Int20 = i & 0xfffff
  inline def truncate(inline i: Int): Int20 = Int20(i)
  inline def truncate(inline i: Long): Int20 = Int20(i.toInt)

  extension (i: Int20)
    inline def toInt: Int = i << 12 >> 12
    inline def repr: UInt20 = UInt20(i)
}

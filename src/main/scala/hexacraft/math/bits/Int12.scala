package hexacraft.math.bits

opaque type Int12 <: AnyVal = Int

object Int12 {
  inline def apply(inline i: Int): Int12 = i & 0xfff
  inline def truncate(inline i: Int): Int12 = Int12(i)
  inline def truncate(inline i: Long): Int12 = Int12(i.toInt)

  extension (i: Int12)
    inline def toInt: Int = i << 20 >> 20
    inline def repr: UInt12 = UInt12(i)
}

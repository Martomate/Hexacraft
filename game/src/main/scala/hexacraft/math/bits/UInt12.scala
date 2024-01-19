package hexacraft.math.bits

opaque type UInt12 <: AnyVal = Int

object UInt12 {
  inline def apply(inline i: Int): UInt12 = i & 0xfff

  inline def truncate(inline i: Int): UInt12 = UInt12(i)
  inline def truncate(inline i: Long): UInt12 = UInt12(i.toInt)

  extension (i: UInt12) {
    inline def toInt: Int = i
  }
}

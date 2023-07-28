package hexacraft.math

opaque type UInt20 <: AnyVal = Int

object UInt20 {
  inline def apply(inline i: Int): UInt20 = i & 0xfffff

  inline def truncate(inline i: Int): UInt20 = UInt20(i)
  inline def truncate(inline i: Long): UInt20 = UInt20(i.toInt)

  extension (i: UInt20) inline def toInt: Int = i
}

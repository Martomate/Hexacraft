package com.martomate.nbt

import com.flowpowered.nbt.{CompoundMap, CompoundTag, Tag}
import com.flowpowered.nbt.stream.{NBTInputStream, NBTOutputStream}
import org.joml.Vector3d

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, IOException}
import scala.collection.immutable.{ArraySeq, ListMap}

sealed trait Nbt {
  def asMap: Option[Nbt.MapTag] = this match {
    case t: Nbt.MapTag => Some(t)
    case _             => None
  }
}

object Nbt {
  case class ByteTag(v: Byte) extends Nbt
  object ByteTag {
    def apply(b: Boolean): ByteTag = ByteTag(if b then 1.toByte else 0.toByte)
  }

  case class ShortTag(v: Short) extends Nbt

  case class IntTag(v: Int) extends Nbt

  case class LongTag(v: Long) extends Nbt

  case class FloatTag(v: Float) extends Nbt

  case class DoubleTag(v: Double) extends Nbt

  case class StringTag(v: String) extends Nbt

  case class ByteArrayTag(vs: ArraySeq.ofByte) extends Nbt
  object ByteArrayTag {
    inline def of(arr: Array[Byte]): ByteArrayTag = ByteArrayTag(ArraySeq.ofByte(arr))
  }

  case class ShortArrayTag(vs: ArraySeq.ofShort) extends Nbt
  object ShortArrayTag {
    inline def of(arr: Array[Short]): ShortArrayTag = ShortArrayTag(ArraySeq.ofShort(arr))
  }

  case class IntArrayTag(vs: ArraySeq.ofInt) extends Nbt
  object IntArrayTag {
    inline def of(arr: Array[Int]): IntArrayTag = IntArrayTag(ArraySeq.ofInt(arr))
  }

  case class ListTag[T <: Nbt](vs: Seq[T]) extends Nbt

  case class MapTag(vs: ListMap[String, Nbt]) extends Nbt {
    def getBoolean(key: String, default: => Boolean): Boolean =
      getByte(key, if default then 1 else 0) == 1

    def getByte(key: String, default: => Byte): Byte =
      vs.get(key) match
        case Some(Nbt.ByteTag(v)) => v
        case _                    => default

    def getShort(key: String, default: => Short): Short =
      vs.get(key) match
        case Some(Nbt.ShortTag(v)) => v
        case _                     => default

    def getLong(key: String, default: => Long): Long =
      vs.get(key) match
        case Some(Nbt.LongTag(v)) => v
        case _                    => default

    def getFloat(key: String, default: => Float): Float =
      vs.get(key) match
        case Some(Nbt.FloatTag(v)) => v
        case _                     => default

    def getDouble(key: String, default: => Double): Double =
      vs.get(key) match
        case Some(Nbt.DoubleTag(v)) => v
        case _                      => default

    def getString(key: String, default: => String): String =
      vs.get(key) match
        case Some(Nbt.StringTag(v)) => v
        case _                      => default

    def getString(key: String): Option[String] =
      vs.get(key) match
        case Some(Nbt.StringTag(v)) => Some(v)
        case _                      => None

    def getList(key: String): Option[Seq[Nbt]] =
      vs.get(key) match
        case Some(Nbt.ListTag(vs)) => Some(vs)
        case _                     => None

    def getTag(name: String): Option[Nbt] =
      vs.get(name) match
        case Some(t) => Some(t)
        case _       => None

    def getMap(name: String): Option[Nbt.MapTag] =
      vs.get(name) match
        case Some(Nbt.MapTag(vs)) => Some(Nbt.MapTag(vs))
        case _                    => None

    def getByteArray(name: String): Option[ArraySeq.ofByte] =
      getTag(name).map(tag => tag.asInstanceOf[Nbt.ByteArrayTag].vs)

    def getShortArray(name: String): Option[ArraySeq.ofShort] =
      getTag(name).map(tag => tag.asInstanceOf[Nbt.ShortArrayTag].vs)

    def setVector(vector: Vector3d): Vector3d =
      val x = getDouble("x", vector.x)
      val y = getDouble("y", vector.y)
      val z = getDouble("z", vector.z)
      vector.set(x, y, z)
  }

  def from(tag: CompoundTag): Nbt.MapTag =
    var map = ListMap.empty[String, Nbt]
    tag.getValue.values().forEach(tag => map += tag.getName -> convertTag(tag))
    Nbt.MapTag(map)

  def emptyMap: Nbt.MapTag = Nbt.MapTag(ListMap.empty)

  def makeMap(elems: (String, Nbt)*): Nbt.MapTag = Nbt.MapTag(ListMap.from(elems))

  def makeVectorTag(vector: Vector3d): Nbt.MapTag =
    Nbt.makeMap(
      "x" -> Nbt.DoubleTag(vector.x),
      "y" -> Nbt.DoubleTag(vector.y),
      "z" -> Nbt.DoubleTag(vector.z)
    )

  def fromBinary(bytes: Array[Byte]): (String, Nbt) =
    val stream = new NBTInputStream(new ByteArrayInputStream(bytes), false)
    try
      val tag = stream.readTag()
      (tag.getName, convertTag(tag))
    catch
      case e: IOException =>
        println(s"${bytes.toSeq} couldn't be read as NBT")
        throw e
    finally stream.close()

  extension (tag: Nbt)
    def toBinary(name: String = ""): Array[Byte] =
      val bytes = new ByteArrayOutputStream()
      new NBTOutputStream(bytes, false).writeTag(tag.toRaw(name))
      bytes.toByteArray

  extension (tag: Nbt.MapTag)
    def toCompoundTag(name: String): CompoundTag =
      val map = new CompoundMap()
      tag.vs.foreach((n, t) => if t != null then map.put(t.toRaw(n)))
      new CompoundTag(name, map)

    def withField(name: String, value: Nbt): Nbt.MapTag =
      MapTag(tag.vs + (name -> value))

    def withOptionalField(name: String, valueOpt: Option[Nbt]): Nbt.MapTag =
      valueOpt match
        case Some(value) => MapTag(tag.vs + (name -> value))
        case None        => tag

  private def convertTag(tag: Tag[?]): Nbt =
    import com.flowpowered.nbt

    tag match
      case t: nbt.ByteTag       => Nbt.ByteTag(t.getValue)
      case t: nbt.ShortTag      => Nbt.ShortTag(t.getValue)
      case t: nbt.IntTag        => Nbt.IntTag(t.getValue)
      case t: nbt.LongTag       => Nbt.LongTag(t.getValue)
      case t: nbt.FloatTag      => Nbt.FloatTag(t.getValue)
      case t: nbt.DoubleTag     => Nbt.DoubleTag(t.getValue)
      case t: nbt.StringTag     => Nbt.StringTag(t.getValue)
      case t: nbt.ByteArrayTag  => Nbt.ByteArrayTag.of(t.getValue)
      case t: nbt.ShortArrayTag => Nbt.ShortArrayTag.of(t.getValue)
      case t: nbt.IntArrayTag   => Nbt.IntArrayTag.of(t.getValue)
      case t: nbt.ListTag[_] =>
        Nbt.ListTag(
          t.getValue
            .stream()
            .map(Nbt.convertTag)
            .toArray(s => new Array[Nbt](s))
            .toSeq
        )
      case t: nbt.CompoundTag => Nbt.from(t)

  extension (t: Nbt)
    def toRaw(name: String): Tag[?] =
      import com.flowpowered.nbt

      t match
        case Nbt.ByteTag(v)        => new nbt.ByteTag(name, v)
        case Nbt.ShortTag(v)       => new nbt.ShortTag(name, v)
        case Nbt.IntTag(v)         => new nbt.IntTag(name, v)
        case Nbt.LongTag(v)        => new nbt.LongTag(name, v)
        case Nbt.FloatTag(v)       => new nbt.FloatTag(name, v)
        case Nbt.DoubleTag(v)      => new nbt.DoubleTag(name, v)
        case Nbt.StringTag(v)      => new nbt.StringTag(name, v)
        case Nbt.ByteArrayTag(vs)  => new nbt.ByteArrayTag(name, vs.toArray)
        case Nbt.ShortArrayTag(vs) => new nbt.ShortArrayTag(name, vs.toArray)
        case Nbt.IntArrayTag(vs)   => new nbt.IntArrayTag(name, vs.toArray)
        case Nbt.ListTag(vs) =>
          import scala.jdk.CollectionConverters.*
          if vs.isEmpty
          then new nbt.ListTag(name, classOf[nbt.EndTag], Nil.asJava)
          else
            new nbt.ListTag(
              name,
              vs.head.toRaw("").getClass.asInstanceOf[Class[Tag[?]]],
              vs.map(_.toRaw("")).toList.asJava
            )
        case tag: Nbt.MapTag => tag.toCompoundTag(name)
}

package com.martomate.hexacraft.util

import com.flowpowered.nbt.*
import com.flowpowered.nbt.stream.{NBTInputStream, NBTOutputStream}
import java.io.{File, IOException}
import java.nio.file.Files
import java.util
import java.util.stream.Collectors
import org.joml.Vector3d
import scala.collection.immutable.{ArraySeq, ListMap}
import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}

enum Nbt {
  case ByteTag(v: Byte)
  case ShortTag(v: Short)
  case IntTag(v: Int)
  case LongTag(v: Long)
  case FloatTag(v: Float)
  case DoubleTag(v: Double)
  case StringTag(v: String)

  case ByteArrayTag(vs: ArraySeq[Byte])
  case ShortArrayTag(vs: ArraySeq[Short])
  case IntArrayTag(vs: ArraySeq[Int])

  case ListTag[T <: Nbt](vs: Seq[T])
  case MapTag(vs: ListMap[String, Nbt])
}

object Nbt {
  def from(tag: CompoundTag): Nbt.MapTag =
    var map = ListMap.empty[String, Nbt]
    tag.getValue.values().forEach(tag => map += tag.getName -> convertTag(tag))
    Nbt.MapTag(map)

  extension (tag: Nbt.MapTag)
    def toCompoundTag(name: String): CompoundTag =
      val map = new CompoundMap()
      tag.vs.foreach((n, t) => map.put(t.toRaw(n)))
      new CompoundTag(name, map)

  def convertTag(tag: Tag[_]): Nbt =
    import com.flowpowered.nbt

    tag match
      case t: nbt.ByteTag       => Nbt.ByteTag(t.getValue)
      case t: nbt.ShortTag      => Nbt.ShortTag(t.getValue)
      case t: nbt.IntTag        => Nbt.IntTag(t.getValue)
      case t: nbt.LongTag       => Nbt.LongTag(t.getValue)
      case t: nbt.FloatTag      => Nbt.FloatTag(t.getValue)
      case t: nbt.DoubleTag     => Nbt.DoubleTag(t.getValue)
      case t: nbt.StringTag     => Nbt.StringTag(t.getValue)
      case t: nbt.ByteArrayTag  => Nbt.ByteArrayTag(ArraySeq.unsafeWrapArray(t.getValue))
      case t: nbt.ShortArrayTag => Nbt.ShortArrayTag(ArraySeq.unsafeWrapArray(t.getValue))
      case t: nbt.IntArrayTag   => Nbt.IntArrayTag(ArraySeq.unsafeWrapArray(t.getValue))
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
          import scala.jdk.CollectionConverters._
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

case class NamedTag(name: String, v: Nbt)

object NBTUtil {
  def getBoolean(tag: CompoundTag, key: String, default: => Boolean): Boolean =
    getByte(tag, key, if (default) 1 else 0) == 1

  def getByte(tag: CompoundTag, key: String, default: => Byte): Byte =
    Nbt.from(tag).vs.get(key) match
      case Some(Nbt.ByteTag(v)) => v
      case _                    => default

  def getShort(tag: CompoundTag, key: String, default: => Short): Short =
    Nbt.from(tag).vs.get(key) match
      case Some(Nbt.ShortTag(v)) => v
      case _                     => default

  def getLong(tag: CompoundTag, key: String, default: => Long): Long =
    Nbt.from(tag).vs.get(key) match
      case Some(Nbt.LongTag(v)) => v
      case _                    => default

  def getDouble(tag: CompoundTag, key: String, default: => Double): Double =
    Nbt.from(tag).vs.get(key) match
      case Some(Nbt.DoubleTag(v)) => v
      case _                      => default

  def getString(tag: CompoundTag, key: String, default: => String): String =
    Nbt.from(tag).vs.get(key) match
      case Some(Nbt.StringTag(v)) => v
      case _                      => default

  def getString(tag: CompoundTag, key: String): Option[String] =
    Nbt.from(tag).vs.get(key) match
      case Some(Nbt.StringTag(v)) => Some(v)
      case _                      => None

  def getList(tag: CompoundTag, key: String): Option[Seq[Tag[_]]] =
    Nbt.from(tag).vs.get(key) match
      case Some(Nbt.ListTag(vs)) => Some(vs.map(_.toRaw("")))
      case _                     => None

  def getTag(tag: CompoundTag, name: String): Option[Tag[_]] =
    Nbt.from(tag).vs.get(name) match
      case Some(t) => Some(t.toRaw(name))
      case _       => None

  def getCompoundTag(tag: CompoundTag, name: String): Option[CompoundTag] =
    Nbt.from(tag).vs.get(name) match
      case Some(Nbt.MapTag(vs)) => Some(Nbt.MapTag(vs).toCompoundTag(name))
      case _                    => None

  def getByteArray(tag: CompoundTag, name: String): Option[ArraySeq[Byte]] =
    NBTUtil
      .getTag(tag, name)
      .map(tag => ArraySeq.unsafeWrapArray(tag.asInstanceOf[ByteArrayTag].getValue))

  def getShortArray(tag: CompoundTag, name: String): Option[ArraySeq[Short]] =
    NBTUtil
      .getTag(tag, name)
      .map(tag => ArraySeq.unsafeWrapArray(tag.asInstanceOf[ShortArrayTag].getValue))

  def setVector(tag: CompoundTag, vector: Vector3d): Vector3d =
    val x = NBTUtil.getDouble(tag, "x", vector.x)
    val y = NBTUtil.getDouble(tag, "y", vector.y)
    val z = NBTUtil.getDouble(tag, "z", vector.z)
    vector.set(x, y, z)

  def makeCompoundTag(name: String, children: Seq[Tag[?]]): CompoundTag =
    Nbt.MapTag(ListMap.from(children.map(t => t.getName -> Nbt.convertTag(t)))).toCompoundTag(name)

  def makeListTag[T <: Tag[_]](name: String, clazz: Class[T], children: Seq[T]): ListTag[T] =
    import scala.jdk.CollectionConverters._
    new ListTag[T](
      name,
      clazz,
      children.toList.asJava
    )

  def makeVectorTag(name: String, vector: Vector3d): CompoundTag = NBTUtil.makeCompoundTag(
    name,
    Seq(
      new DoubleTag("x", vector.x),
      new DoubleTag("y", vector.y),
      new DoubleTag("z", vector.z)
    )
  )

  def saveTag(tag: Tag[_], nbtFile: File): Unit =
    AsyncFileIO.submit(
      nbtFile,
      nbtFile => {
        nbtFile.getParentFile.mkdirs()

        val nbtOut = new NBTOutputStream(Files.newOutputStream(nbtFile.toPath))
        try nbtOut.writeTag(tag)
        finally nbtOut.close()
      }
    )

  def loadTag(file: File): CompoundTag =
    if file.isFile then
      val readOperation = AsyncFileIO.submit(
        file,
        file => {
          val stream = new NBTInputStream(Files.newInputStream(file.toPath))
          try stream.readTag().asInstanceOf[CompoundTag]
          catch
            case e: IOException =>
              println(file.getAbsolutePath + " couldn't be read as NBT")
              throw e
          finally stream.close()
        }
      )
      Await.result(readOperation, Duration(5, SECONDS))
    else new CompoundTag("", new CompoundMap())
}

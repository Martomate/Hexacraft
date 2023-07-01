package com.martomate.hexacraft.util

import com.flowpowered.nbt.*
import com.flowpowered.nbt.stream.{NBTInputStream, NBTOutputStream}
import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File, IOException}
import java.nio.file.Files
import java.util
import java.util.stream.Collectors
import org.joml.Vector3d
import scala.collection.immutable.{ArraySeq, ListMap}
import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}

object NBTUtil {
  def getBoolean(tag: Nbt.MapTag, key: String, default: => Boolean): Boolean =
    getByte(tag, key, if default then 1 else 0) == 1

  def getByte(tag: Nbt.MapTag, key: String, default: => Byte): Byte =
    tag.vs.get(key) match
      case Some(Nbt.ByteTag(v)) => v
      case _                    => default

  def getShort(tag: Nbt.MapTag, key: String, default: => Short): Short =
    tag.vs.get(key) match
      case Some(Nbt.ShortTag(v)) => v
      case _                     => default

  def getLong(tag: Nbt.MapTag, key: String, default: => Long): Long =
    tag.vs.get(key) match
      case Some(Nbt.LongTag(v)) => v
      case _                    => default

  def getDouble(tag: Nbt.MapTag, key: String, default: => Double): Double =
    tag.vs.get(key) match
      case Some(Nbt.DoubleTag(v)) => v
      case _                      => default

  def getString(tag: Nbt.MapTag, key: String, default: => String): String =
    tag.vs.get(key) match
      case Some(Nbt.StringTag(v)) => v
      case _                      => default

  def getString(tag: Nbt.MapTag, key: String): Option[String] =
    tag.vs.get(key) match
      case Some(Nbt.StringTag(v)) => Some(v)
      case _                      => None

  def getList(tag: Nbt.MapTag, key: String): Option[Seq[Nbt]] =
    tag.vs.get(key) match
      case Some(Nbt.ListTag(vs)) => Some(vs)
      case _                     => None

  def getTag(tag: Nbt.MapTag, name: String): Option[Nbt] =
    tag.vs.get(name) match
      case Some(t) => Some(t)
      case _       => None

  def getCompoundTag(tag: Nbt.MapTag, name: String): Option[Nbt.MapTag] =
    tag.vs.get(name) match
      case Some(Nbt.MapTag(vs)) => Some(Nbt.MapTag(vs))
      case _                    => None

  def getByteArray(tag: Nbt.MapTag, name: String): Option[ArraySeq[Byte]] =
    NBTUtil
      .getTag(tag, name)
      .map(tag => tag.asInstanceOf[Nbt.ByteArrayTag].vs)

  def getShortArray(tag: Nbt.MapTag, name: String): Option[ArraySeq[Short]] =
    NBTUtil
      .getTag(tag, name)
      .map(tag => tag.asInstanceOf[Nbt.ShortArrayTag].vs)

  def setVector(tag: Nbt.MapTag, vector: Vector3d): Vector3d =
    val x = NBTUtil.getDouble(tag, "x", vector.x)
    val y = NBTUtil.getDouble(tag, "y", vector.y)
    val z = NBTUtil.getDouble(tag, "z", vector.z)
    vector.set(x, y, z)

  def makeCompoundTag(name: String, children: Seq[Tag[?]]): CompoundTag =
    Nbt.MapTag(ListMap.from(children.map(t => t.getName -> Nbt.convertTag(t)))).toCompoundTag(name)

  def makeListTag[T <: Nbt](name: String, clazz: Class[T], children: Seq[T]): Nbt.ListTag[T] =
    Nbt.ListTag[T](children)

  def makeVectorTag(name: String, vector: Vector3d): CompoundTag = NBTUtil.makeCompoundTag(
    name,
    Seq(
      new DoubleTag("x", vector.x),
      new DoubleTag("y", vector.y),
      new DoubleTag("z", vector.z)
    )
  )
}
